package com.yh.toy_pj.domain.user.dto;

import com.yh.toy_pj.domain.user.User;
import com.yh.toy_pj.domain.user.UserRole;
import java.time.LocalDateTime;

/**
 * @param mustChangePassword 임시 비밀번호로 로그인한 상태 → 프론트엔드가 비밀번호 변경 화면으로 안내
 * @param lockedUntil        로그인 잠금 해제 시각 (잠기지 않았으면 null)
 */
public record UserResponse(Long id, String name, String email, String department, UserRole role,
                           boolean mustChangePassword, LocalDateTime lockedUntil) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getDepartment(), user.getRole(),
                user.isMustChangePassword(), user.getLockedUntil());
    }
}
