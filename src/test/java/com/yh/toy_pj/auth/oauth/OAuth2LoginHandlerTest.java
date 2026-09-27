package com.yh.toy_pj.auth.oauth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.yh.toy_pj.auth.AuthService;
import com.yh.toy_pj.auth.RefreshCookieFactory;
import com.yh.toy_pj.domain.user.SocialProvider;
import com.yh.toy_pj.domain.user.User;
import com.yh.toy_pj.global.error.BusinessException;
import com.yh.toy_pj.global.error.ErrorCode;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.test.util.ReflectionTestUtils;

/** SNS 로그인 성공·실패 처리기: 쿠키와 리다이렉트 주소만 검증한다 (회원 처리는 SocialLoginIntegrationTest). */
class OAuth2LoginHandlerTest {

    private final SocialLoginService socialLoginService = mock(SocialLoginService.class);
    private final AuthService authService = mock(AuthService.class);
    private final RefreshCookieFactory cookieFactory = mock(RefreshCookieFactory.class);
    private final OAuth2LoginSuccessHandler successHandler =
            new OAuth2LoginSuccessHandler(socialLoginService, authService, cookieFactory);
    private final OAuth2LoginFailureHandler failureHandler = new OAuth2LoginFailureHandler();

    private final MockHttpServletRequest request = new MockHttpServletRequest();
    private final MockHttpServletResponse response = new MockHttpServletResponse();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(successHandler, "successRedirectUrl", "/oauth/callback");
        ReflectionTestUtils.setField(successHandler, "failureRedirectUrl", "/login");
        ReflectionTestUtils.setField(failureHandler, "failureRedirectUrl", "/login");
    }

    /** Spring 이 Google 사용자 정보를 받아 만든 인증 결과를 흉내 낸다. */
    private static OAuth2AuthenticationToken googleAuthentication() {
        var authorities = List.of(new SimpleGrantedAuthority("OAUTH2_USER"));
        var principal = new DefaultOAuth2User(authorities,
                Map.of("sub", "109876543210", "name", "홍길동", "email", "hong@gmail.com", "email_verified", true), "sub");
        return new OAuth2AuthenticationToken(principal, authorities, "google");
    }

    @Test
    @DisplayName("성공: 제공자 응답을 변환해 로그인시키고, refresh 쿠키만 심은 뒤 콜백 페이지로 보낸다 (URL 에 토큰 없음)")
    void successSetsCookieAndRedirects() throws Exception {
        User user = mock(User.class);
        given(socialLoginService.loginOrSignup(any())).willReturn(user);
        given(authService.socialLogin(user)).willReturn(new AuthService.LoginResult(null, "raw-refresh-token"));
        given(cookieFactory.create("raw-refresh-token"))
                .willReturn(ResponseCookie.from(RefreshCookieFactory.NAME, "raw-refresh-token").httpOnly(true).build());

        successHandler.onAuthenticationSuccess(request, response, googleAuthentication());

        verify(socialLoginService).loginOrSignup(new OAuthUserInfo(SocialProvider.GOOGLE, "109876543210", "홍길동", "hong@gmail.com"));
        assertThat(response.getHeader(HttpHeaders.SET_COOKIE)).startsWith("refresh_token=raw-refresh-token").contains("HttpOnly");
        assertThat(response.getRedirectedUrl()).isEqualTo("/oauth/callback");
    }

    @Test
    @DisplayName("회원 처리에서 거부되면(이미 가입된 이메일 등) 쿠키 없이 에러 코드를 붙여 로그인 화면으로 보낸다")
    void rejectedRedirectsWithErrorCode() throws Exception {
        given(socialLoginService.loginOrSignup(any()))
                .willThrow(new BusinessException(ErrorCode.SOCIAL_EMAIL_ALREADY_REGISTERED));

        successHandler.onAuthenticationSuccess(request, response, googleAuthentication());

        verify(authService, never()).socialLogin(any());
        assertThat(response.getHeader(HttpHeaders.SET_COOKIE)).isNull();
        assertThat(response.getRedirectedUrl()).isEqualTo("/login?error=AUTH010");
    }

    @Test
    @DisplayName("예상 못 한 오류(동시 가입으로 UNIQUE 위반 등)도 500 화면 대신 AUTH011 로 로그인 화면에 보낸다")
    void unexpectedErrorRedirects() throws Exception {
        given(socialLoginService.loginOrSignup(any()))
                .willThrow(new org.springframework.dao.DataIntegrityViolationException("uk_social_provider_user"));

        successHandler.onAuthenticationSuccess(request, response, googleAuthentication());

        assertThat(response.getHeader(HttpHeaders.SET_COOKIE)).isNull();
        assertThat(response.getRedirectedUrl()).isEqualTo("/login?error=AUTH011");
    }

    @Test
    @DisplayName("제공자 쪽 인증 실패(동의 취소 등)는 AUTH011 을 붙여 로그인 화면으로 보낸다")
    void providerFailure() throws Exception {
        failureHandler.onAuthenticationFailure(request, response,
                new OAuth2AuthenticationException(new OAuth2Error("access_denied")));

        assertThat(response.getRedirectedUrl()).isEqualTo("/login?error=AUTH011");
    }
}
