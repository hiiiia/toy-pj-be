package com.yh.toy_pj.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.yh.toy_pj.domain.user.User;
import com.yh.toy_pj.domain.user.UserRole;
import com.yh.toy_pj.support.IntegrationTestSupport;
import jakarta.servlet.http.Cookie;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

/** 로그인 시도 제한, 비밀번호 변경, 관리자 초기화·잠금 해제 */
class AccountSecurityIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    private User hong;
    private String admin;

    @BeforeEach
    void setUp() throws Exception {
        hong = createUser("홍길동", "hong@daon.example", UserRole.USER);
        createUser("김관리", "admin@daon.example", UserRole.ADMIN);
        admin = login("admin@daon.example");
    }

    @Test
    @DisplayName("5번 연속 틀리면 423 AUTH005 로 잠기고, 잠긴 동안에는 맞는 비밀번호도 거절된다")
    void lockAfterFiveFailures() throws Exception {
        for (int i = 1; i <= 4; i++) {
            tryLogin("hong@daon.example", "wrong-pass" + i).andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("AUTH002"));
        }
        // 실패 횟수는 예외가 나도 롤백되지 않고 저장된다 (noRollbackFor)
        assertThat(userRepository.findByEmail("hong@daon.example").orElseThrow().getFailedLoginCount()).isEqualTo(4);

        tryLogin("hong@daon.example", "wrong-pass5")
                .andExpect(status().isLocked())
                .andExpect(jsonPath("$.code").value("AUTH005"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("15분")));

        tryLogin("hong@daon.example", PASSWORD).andExpect(status().isLocked());

        // 관리자가 잠금 해제 → 바로 로그인 가능
        mockMvc.perform(post("/api/users/" + hong.getId() + "/unlock").header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lockedUntil").doesNotExist());
        tryLogin("hong@daon.example", PASSWORD).andExpect(status().isOk());
    }

    @Test
    @DisplayName("비밀번호 변경: 현재 비밀번호 확인, 다른 기기 세션 폐기, 이 기기에는 새 토큰 발급")
    void changePassword() throws Exception {
        Cookie otherDevice = tryLogin("hong@daon.example", PASSWORD).andReturn().getResponse().getCookie("refresh_token");
        String token = login("hong@daon.example");

        changePassword(token, "wrong-current1", "newpass123")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("AUTH006"));
        changePassword(token, PASSWORD, PASSWORD)
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("AUTH007"));
        changePassword(token, PASSWORD, "short")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors[0].field").value("newPassword"));

        changePassword(token, PASSWORD, "newpass123")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isString());

        // 다른 기기의 refresh token 은 폐기됨
        mockMvc.perform(post("/api/auth/refresh").cookie(otherDevice)).andExpect(status().isUnauthorized());
        assertThat(refreshTokenRepository.count()).isEqualTo(2); // 관리자 1 + 방금 새로 받은 1

        tryLogin("hong@daon.example", PASSWORD).andExpect(status().isUnauthorized());
        tryLogin("hong@daon.example", "newpass123").andExpect(status().isOk());
    }

    @Test
    @DisplayName("관리자 초기화: 임시 비밀번호는 응답에서만 확인, DB 에는 해시, 로그인하면 변경 요구 표시")
    void adminResetPassword() throws Exception {
        String body = mockMvc.perform(post("/api/users/" + hong.getId() + "/password-reset")
                        .header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String temporary = JsonPath.read(body, "$.temporaryPassword");

        User saved = userRepository.findByEmail("hong@daon.example").orElseThrow();
        assertThat(saved.getPassword()).startsWith("{bcrypt}").doesNotContain(temporary);

        String tempBody = tryLogin("hong@daon.example", temporary)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.mustChangePassword").value(true))
                .andReturn().getResponse().getContentAsString();
        String tempToken = "Bearer " + JsonPath.read(tempBody, "$.accessToken");

        // 임시 비밀번호 상태에서는 서버도 다른 API 를 막는다 (프론트 화면 이동만으로는 API 직접 호출을 막을 수 없음)
        mockMvc.perform(get("/api/tickets").header(HttpHeaders.AUTHORIZATION, tempToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH008"));
        mockMvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, tempToken))
                .andExpect(status().isOk());

        // 비밀번호를 바꾸면 새로 받은 토큰으로 모든 기능 사용 가능
        String changed = changePassword(tempToken, temporary, "newpass123")
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        mockMvc.perform(get("/api/tickets").header(HttpHeaders.AUTHORIZATION, "Bearer " + JsonPath.read(changed, "$.accessToken")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("비밀번호가 바뀌면 그 전에 발급된 access token 은 만료 전이라도 거절된다 (401)")
    void tokenIssuedBeforePasswordChangeIsRevoked() throws Exception {
        String oldToken = login("hong@daon.example");
        mockMvc.perform(get("/api/tickets").header(HttpHeaders.AUTHORIZATION, oldToken)).andExpect(status().isOk());

        // 토큰 발급 이후에 비밀번호가 바뀐 상황 (예: 관리자 초기화, 다른 기기에서 변경)
        User user = userRepository.findByEmail("hong@daon.example").orElseThrow();
        user.changePassword(user.getPassword(), LocalDateTime.now(ZoneId.of("Asia/Seoul")).plusMinutes(1));
        userRepository.save(user);

        mockMvc.perform(get("/api/tickets").header(HttpHeaders.AUTHORIZATION, oldToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH001"));
    }

    @Test
    @DisplayName("일반 사용자는 다른 사람의 비밀번호를 초기화할 수 없다")
    void onlyAdminCanReset() throws Exception {
        String employee = login("hong@daon.example");
        mockMvc.perform(post("/api/users/" + hong.getId() + "/password-reset").header(HttpHeaders.AUTHORIZATION, employee))
                .andExpect(status().isForbidden());
    }

    private ResultActions tryLogin(String email, String password) throws Exception {
        return mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\": \"%s\", \"password\": \"%s\"}".formatted(email, password)));
    }

    private ResultActions changePassword(String token, String current, String next) throws Exception {
        return mockMvc.perform(patch("/api/auth/password").header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"currentPassword\": \"%s\", \"newPassword\": \"%s\"}".formatted(current, next)));
    }
}
