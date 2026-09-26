package com.yh.toy_pj.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Refresh token 생성 및 해시.
 *
 * 비밀번호는 BCrypt(느린 해시 + salt)를 쓰지만 refresh token 은 SHA-256(빠른 해시)으로 충분하다.
 * - 비밀번호: 사람이 정해서 추측 가능 → 대입 공격을 늦추려고 일부러 느린 해시를 사용
 * - refresh token: 서버가 만든 256비트 무작위 값 → 추측 자체가 불가능하므로 빠른 해시로도 안전하고,
 *   해시값이 항상 같아서 DB 에서 바로 조회(인덱스 검색)할 수 있다.
 */
public final class TokenHasher {

    private static final SecureRandom RANDOM = new SecureRandom();

    private TokenHasher() {
    }

    /** 추측 불가능한 256비트 무작위 토큰 (클라이언트에게만 전달, DB 에는 저장하지 않음) */
    public static String newToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** DB 에 저장·조회할 SHA-256 해시 (64자리 16진수) */
    public static String sha256(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 을 사용할 수 없습니다.", e);
        }
    }
}
