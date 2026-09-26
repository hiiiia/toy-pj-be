package com.yh.toy_pj.domain.user.dto;

/**
 * 임시 비밀번호는 이 응답에서 딱 한 번만 보여준다. DB 에는 BCrypt 해시만 저장된다.
 */
public record PasswordResetResponse(Long userId, String temporaryPassword) {
}
