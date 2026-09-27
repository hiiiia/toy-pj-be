package com.yh.toy_pj.auth.oauth;

import com.yh.toy_pj.auth.AuthService;
import com.yh.toy_pj.auth.RefreshCookieFactory;
import com.yh.toy_pj.domain.user.User;
import com.yh.toy_pj.global.error.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

/**
 * SNS 인증이 끝나면 Spring Security 가 호출한다.
 * 우리 회원으로 로그인시키고 refresh token 쿠키만 심은 뒤 프론트로 돌려보낸다.
 * (access token 은 URL 에 싣지 않는다 → 프론트가 /api/auth/refresh 로 받아간다)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final SocialLoginService socialLoginService;
    private final AuthService authService;
    private final RefreshCookieFactory cookieFactory;

    @Value("${app.oauth.success-redirect-url}")
    private String successRedirectUrl;

    @Value("${app.oauth.failure-redirect-url}")
    private String failureRedirectUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        // ① Spring 이 넘겨준 인증 결과에서 "어느 제공자(kakao)"와 "사용자 정보 Map" 꺼내기
        OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) authentication;
        String registrationId = token.getAuthorizedClientRegistrationId();
        Map<String, Object> attributes = token.getPrincipal().getAttributes();

        try {
            OAuthUserInfo info = OAuthUserInfo.of(registrationId, attributes);          // ② 3사 응답 → 공통 모양
            User user = socialLoginService.loginOrSignup(info);                        // ③ 회원 찾기/가입
            AuthService.LoginResult result = authService.socialLogin(user);            // ④ 토큰 발급
            response.addHeader(HttpHeaders.SET_COOKIE,
                    cookieFactory.create(result.refreshToken()).toString());           // ⑤ refresh 쿠키

            response.sendRedirect(successRedirectUrl);                                 // ⑥ 프론트로
        } catch (BusinessException e) {
            // 이메일 미동의(AUTH009), 이미 가입된 이메일(AUTH010) 등
            // 여기는 Controller 가 아니라서 GlobalExceptionHandler 가 받지 못한다 → 직접 에러 코드를 붙여 로그인 화면으로
            log.info("SNS 로그인 거부: provider={}, code={}", registrationId, e.getErrorCode().getCode());
            response.sendRedirect(failureRedirectUrl + "?error=" + e.getErrorCode().getCode());
        }
    }
}