package com.yh.toy_pj.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.yh.toy_pj.domain.user.User;
import com.yh.toy_pj.domain.user.UserRole;
import com.yh.toy_pj.support.IntegrationTestSupport;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

class AuthIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Test
    @DisplayName("회원가입: 비밀번호는 BCrypt 해시로 저장되고, 역할은 항상 USER")
    void signupStoresHashedPassword() throws Exception {
        mockMvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON).content("""
                        {"name": "신입", "email": "new@daon.example", "password": "secret123", "department": "개발팀"}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.password").doesNotExist());

        User saved = userRepository.findByEmail("new@daon.example").orElseThrow();
        assertThat(saved.getPassword())
                .isNotEqualTo("secret123")
                .startsWith("{bcrypt}$2a$");
        assertThat(passwordEncoder.matches("secret123", saved.getPassword())).isTrue();
    }

    @Test
    @DisplayName("같은 비밀번호라도 salt 때문에 해시 값은 매번 다르다")
    void samePasswordDifferentHash() {
        User a = createUser("A", "a@daon.example", UserRole.USER);
        User b = createUser("B", "b@daon.example", UserRole.USER);

        assertThat(a.getPassword()).isNotEqualTo(b.getPassword());
    }

    @Test
    @DisplayName("비밀번호 규칙(영문+숫자 8자 이상)을 지키지 않으면 400")
    void weakPassword() throws Exception {
        mockMvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON).content("""
                        {"name": "신입", "email": "new@daon.example", "password": "12345678"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("password"));
    }

    @Test
    @DisplayName("로그인: access token 은 본문, refresh token 은 HttpOnly 쿠키. DB 에는 refresh token 의 해시만 저장")
    void loginIssuesTokens() throws Exception {
        createUser("홍길동", "hong@daon.example", UserRole.USER);

        MvcResult result = login("hong@daon.example", PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.accessToken").isString())
                .andExpect(jsonPath("$.user.name").value("홍길동"))
                .andExpect(cookie().httpOnly("refresh_token", true))
                .andExpect(cookie().path("refresh_token", "/api/auth"))
                .andReturn();

        String rawRefreshToken = result.getResponse().getCookie("refresh_token").getValue();
        RefreshToken stored = refreshTokenRepository.findAll().get(0);
        assertThat(stored.getTokenHash())
                .isNotEqualTo(rawRefreshToken)
                .isEqualTo(TokenHasher.sha256(rawRefreshToken))
                .hasSize(64);
    }

    @Test
    @DisplayName("잘못된 비밀번호와 없는 이메일은 같은 메시지로 401 (계정 존재 여부를 노출하지 않음)")
    void invalidCredentials() throws Exception {
        createUser("홍길동", "hong@daon.example", UserRole.USER);

        login("hong@daon.example", "wrong-pass1")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH002"));
        login("nobody@daon.example", PASSWORD)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH002"));
    }

    @Test
    @DisplayName("토큰 재발급(rotation): 새 토큰을 받고, 한 번 쓴 refresh token 은 다시 쓸 수 없다")
    void refreshRotation() throws Exception {
        createUser("홍길동", "hong@daon.example", UserRole.USER);
        Cookie first = login("hong@daon.example", PASSWORD).andReturn().getResponse().getCookie("refresh_token");

        MvcResult refreshed = mockMvc.perform(post("/api/auth/refresh").cookie(first))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isString())
                .andReturn();
        Cookie second = refreshed.getResponse().getCookie("refresh_token");
        assertThat(second.getValue()).isNotEqualTo(first.getValue());

        // 이미 사용한(폐기된) refresh token 재사용 → 401
        mockMvc.perform(post("/api/auth/refresh").cookie(first))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH003"));
    }

    @Test
    @DisplayName("로그아웃하면 refresh token 이 폐기되고 쿠키가 삭제된다")
    void logout() throws Exception {
        createUser("홍길동", "hong@daon.example", UserRole.USER);
        Cookie refresh = login("hong@daon.example", PASSWORD).andReturn().getResponse().getCookie("refresh_token");

        mockMvc.perform(post("/api/auth/logout").cookie(refresh))
                .andExpect(status().isNoContent())
                .andExpect(cookie().maxAge("refresh_token", 0));

        assertThat(refreshTokenRepository.count()).isZero();
        mockMvc.perform(post("/api/auth/refresh").cookie(refresh))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("내 정보: 토큰의 사용자 정보를 반환하고, 토큰이 없으면 401")
    void me() throws Exception {
        createUser("김관리", "admin@daon.example", UserRole.ADMIN);
        String token = login("admin@daon.example");

        mockMvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("admin@daon.example"))
                .andExpect(jsonPath("$.role").value("ADMIN"));
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH001"));
    }

    private org.springframework.test.web.servlet.ResultActions login(String email, String password) throws Exception {
        return mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\": \"%s\", \"password\": \"%s\"}".formatted(email, password)));
    }
}
