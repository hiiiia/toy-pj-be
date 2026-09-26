package com.yh.toy_pj.auth.jwt;

import com.yh.toy_pj.auth.AuthUser;
import com.yh.toy_pj.domain.user.User;
import com.yh.toy_pj.domain.user.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.DecodingException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import javax.crypto.SecretKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Access token(JWT) 발급과 검증.
 *
 * <pre>
 * payload 예시: { "sub": "1", "name": "김관리", "role": "ADMIN", "iat": ..., "exp": ... }
 * </pre>
 * 서명(HS256)으로 위변조를 막는다. payload 는 암호화가 아니라 Base64 인코딩일 뿐이므로 민감 정보는 넣지 않는다.
 */
@Slf4j
@Component
public class JwtTokenProvider {

    private static final String CLAIM_NAME = "name";
    private static final String CLAIM_ROLE = "role";

    private final SecretKey key;
    private final JwtProperties properties;
    private final Clock clock;

    public JwtTokenProvider(JwtProperties properties, Clock clock) {
        String secret = properties.secret();
        // 환경변수가 없으면 "${JWT_SECRET}" 문자열이 그대로 들어올 수 있어 함께 검사한다
        if (!StringUtils.hasText(secret) || secret.startsWith("${")) {
            throw new IllegalStateException("JWT 서명 키가 없습니다. 환경변수 JWT_SECRET 을 설정하세요. (생성: openssl rand -base64 32)");
        }
        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(secret);
        } catch (DecodingException e) {
            throw new IllegalStateException("JWT_SECRET 이 올바른 Base64 값이 아닙니다. (생성: openssl rand -base64 32)", e);
        }
        if (keyBytes.length < 32) {
            throw new IllegalStateException("JWT_SECRET 은 Base64 로 인코딩된 32바이트(256비트) 이상이어야 합니다.");
        }
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.properties = properties;
        this.clock = clock;
    }

    public String createAccessToken(User user) {
        Date now = Date.from(clock.instant());
        Date expiry = Date.from(clock.instant().plus(properties.accessTokenTtl()));
        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim(CLAIM_NAME, user.getName())
                .claim(CLAIM_ROLE, user.getRole().name())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    /** 검증을 통과한 access token 의 내용: 사용자 정보 + 발급 시각 */
    public record AccessToken(AuthUser user, Instant issuedAt) {
    }

    /** 서명·만료를 검증하고 사용자 정보를 꺼낸다. 유효하지 않으면 빈 값. */
    public Optional<AccessToken> parse(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .clock(() -> Date.from(clock.instant()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            AuthUser user = new AuthUser(
                    Long.valueOf(claims.getSubject()),
                    claims.get(CLAIM_NAME, String.class),
                    UserRole.valueOf(claims.get(CLAIM_ROLE, String.class)));
            return Optional.of(new AccessToken(user, claims.getIssuedAt().toInstant()));
        } catch (JwtException | IllegalArgumentException | NullPointerException e) { // iat 가 없는 토큰 포함
            log.debug("유효하지 않은 JWT: {}", e.getMessage());
            return Optional.empty();
        }
    }

    public long accessTokenTtlSeconds() {
        return properties.accessTokenTtl().toSeconds();
    }
}
