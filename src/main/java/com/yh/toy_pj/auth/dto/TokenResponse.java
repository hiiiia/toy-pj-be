package com.yh.toy_pj.auth.dto;

import com.yh.toy_pj.domain.user.dto.UserResponse;

/**
 * 로그인/재발급 응답. refresh token 은 본문이 아닌 HttpOnly 쿠키로 내려가므로 여기에 없다.
 */
public record TokenResponse(String accessToken, String tokenType, long expiresIn, UserResponse user) {

    public static TokenResponse bearer(String accessToken, long expiresIn, UserResponse user) {
        return new TokenResponse(accessToken, "Bearer", expiresIn, user);
    }
}
