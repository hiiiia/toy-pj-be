package com.yh.toy_pj.auth.oauth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.yh.toy_pj.auth.AuthService;
import com.yh.toy_pj.domain.user.SocialAccountRepository;
import com.yh.toy_pj.domain.user.SocialProvider;
import com.yh.toy_pj.domain.user.User;
import com.yh.toy_pj.domain.user.UserRole;
import com.yh.toy_pj.global.error.BusinessException;
import com.yh.toy_pj.global.error.ErrorCode;
import com.yh.toy_pj.support.IntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

/**
 * SNS 로그인 이후 단계(회원 찾기/가입, 토큰 발급)와 기존 이메일 로그인 규칙과의 관계를 실제 DB 로 검증한다.
 * 제공자(Google 등)와의 통신은 Spring Security 가 담당하므로 여기서는 변환이 끝난 OAuthUserInfo 부터 시작한다.
 */
class SocialLoginIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private SocialLoginService socialLoginService;

    @Autowired
    private SocialAccountRepository socialAccountRepository;

    @Autowired
    private AuthService authService;

    private static OAuthUserInfo kakao(String email) {
        return new OAuthUserInfo(SocialProvider.KAKAO, "3812345678", "길동이", email);
    }

    @Test
    @DisplayName("처음 들어온 SNS 계정은 비밀번호 없는 일반 사용자(USER)로 가입되고 연결 정보가 저장된다")
    void signupOnFirstLogin() {
        User user = socialLoginService.loginOrSignup(kakao("gildong@kakao.com"));

        assertThat(user.getId()).isNotNull();
        assertThat(user.getRole()).isEqualTo(UserRole.USER);
        assertThat(user.hasPassword()).isFalse();
        assertThat(user.getEmail()).isEqualTo("gildong@kakao.com");
        assertThat(socialAccountRepository.findByProviderAndProviderUserId(SocialProvider.KAKAO, "3812345678"))
                .get().extracting(account -> account.getUser().getId()).isEqualTo(user.getId());
    }

    @Test
    @DisplayName("같은 SNS 계정으로 다시 로그인하면 새로 가입하지 않고 기존 회원을 돌려준다")
    void reLoginReturnsSameUser() {
        User first = socialLoginService.loginOrSignup(kakao("gildong@kakao.com"));
        long usersAfterFirst = userRepository.count();

        User second = socialLoginService.loginOrSignup(kakao("gildong@kakao.com"));

        assertThat(second.getId()).isEqualTo(first.getId());
        assertThat(userRepository.count()).isEqualTo(usersAfterFirst);
    }

    @Test
    @DisplayName("이메일 제공에 동의하지 않은 SNS 계정은 가입을 거부한다 (AUTH009)")
    void emailRequired() {
        assertThatThrownBy(() -> socialLoginService.loginOrSignup(kakao(null)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.SOCIAL_EMAIL_REQUIRED);
    }

    @Test
    @DisplayName("이미 이메일로 가입한 주소면 자동으로 연결하지 않고 거부한다 (대소문자가 달라도 같은 이메일, AUTH010)")
    void emailAlreadyRegistered() {
        createUser("홍길동", "hong@daon.example", UserRole.USER);

        assertThatThrownBy(() -> socialLoginService.loginOrSignup(kakao("Hong@Daon.example")))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.SOCIAL_EMAIL_ALREADY_REGISTERED);
        assertThat(socialAccountRepository.count()).isZero();
    }

    @Test
    @DisplayName("SNS 로그인으로 받은 토큰으로 일반 API 를 호출할 수 있다")
    void socialLoginTokenWorks() throws Exception {
        User user = socialLoginService.loginOrSignup(kakao("gildong@kakao.com"));
        String token = "Bearer " + authService.socialLogin(user).body().accessToken();

        mockMvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("gildong@kakao.com"));
    }

    @Test
    @DisplayName("SNS 로만 가입한 계정에 이메일/비밀번호 로그인을 시도하면 500 이 아니라 일반 로그인 실패(401 AUTH002)")
    void socialOnlyAccountCannotUsePasswordLogin() throws Exception {
        socialLoginService.loginOrSignup(kakao("gildong@kakao.com"));

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"gildong@kakao.com\", \"password\": \"anything1\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH002"));
    }

    @Test
    @DisplayName("SNS 로만 가입한 계정은 비밀번호를 변경할 수 없다 (400 AUTH012)")
    void socialOnlyAccountCannotChangePassword() throws Exception {
        User user = socialLoginService.loginOrSignup(kakao("gildong@kakao.com"));
        String token = "Bearer " + authService.socialLogin(user).body().accessToken();

        mockMvc.perform(patch("/api/auth/password").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\": \"anything1\", \"newPassword\": \"newpass123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("AUTH012"));
    }
}
