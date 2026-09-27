package com.yh.toy_pj.auth;

import com.yh.toy_pj.auth.jwt.JwtProperties;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/** refresh token 쿠키의 모양(이름·경로·보안 속성)을 한 곳에서 관리한다. 일반 로그인과 SNS 로그인이 함께 쓴다. */
@Component
@RequiredArgsConstructor
public class RefreshCookieFactory {

    public static final String NAME = "refresh_token";
    private static final String PATH = "/api/auth";

    private final JwtProperties jwtProperties;

    /** 로그인 성공 시 발급할 refresh token 쿠키 */
    public ResponseCookie create(String refreshToken) {
        return build(refreshToken, jwtProperties.refreshTokenTtl());
    }

    /** 로그아웃 시 쿠키 삭제용 (값 비움, 만료 0) */
    public ResponseCookie expire() {
        return build("", Duration.ZERO);
    }

    private ResponseCookie build(String value, Duration maxAge) {
        return ResponseCookie.from(NAME, value)
                .httpOnly(true)                              // JS 에서 읽지 못하게 (XSS 로 탈취 방지)
                .secure(jwtProperties.cookieSecure())        // 운영(HTTPS)에서만 true
                .sameSite("Strict")                          // 다른 사이트에서 보낸 요청에는 쿠키를 붙이지 않음 (CSRF 방지)
                .path(PATH)                                  // /api/auth/** 요청에만 전송
                .maxAge(maxAge)
                .build();
    }
}