package com.yh.toy_pj.auth.oauth;

import com.yh.toy_pj.global.error.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

/** 제공자 쪽에서 인증이 실패했을 때 (동의 화면에서 취소, 잘못된 state 등) → 로그인 화면으로 */
@Slf4j
@Component
public class OAuth2LoginFailureHandler implements AuthenticationFailureHandler {

    @Value("${app.oauth.failure-redirect-url}")
    private String failureRedirectUrl;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        log.info("SNS 인증 실패: {}", exception.getMessage());
        response.sendRedirect(failureRedirectUrl + "?error=" + ErrorCode.SOCIAL_LOGIN_FAILED.getCode());
    }
}
