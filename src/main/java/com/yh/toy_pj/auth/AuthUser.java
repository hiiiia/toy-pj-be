package com.yh.toy_pj.auth;

import com.yh.toy_pj.domain.user.UserRole;

/**
 * 로그인한 사용자 정보. JWT 에서 꺼내 SecurityContext 에 보관하고,
 * 컨트롤러에서는 {@code @AuthenticationPrincipal AuthUser me} 로 받는다.
 * 매 요청마다 DB 를 조회하지 않도록 토큰에 담긴 최소 정보만 가진다.
 */
public record AuthUser(Long id, String name, UserRole role) {

    public boolean isAdmin() {
        return role == UserRole.ADMIN;
    }
}
