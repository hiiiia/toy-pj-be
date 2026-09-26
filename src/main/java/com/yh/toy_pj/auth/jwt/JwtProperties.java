package com.yh.toy_pj.auth.jwt;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * @param secret          HMAC-SHA256 서명 키 (Base64, 32바이트 이상). 반드시 환경변수 JWT_SECRET 으로 주입한다.
 * @param accessTokenTtl  access token 유효 시간. 짧게 두어 탈취되더라도 피해 시간을 줄인다.
 * @param refreshTokenTtl refresh token 유효 시간 (로그인 유지 기간)
 * @param cookieSecure    refresh token 쿠키를 HTTPS 에서만 전송할지 여부 (운영 환경에서는 true)
 */
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
        String secret,
        @DefaultValue("30m") Duration accessTokenTtl,
        @DefaultValue("14d") Duration refreshTokenTtl,
        @DefaultValue("false") boolean cookieSecure
) {
}
