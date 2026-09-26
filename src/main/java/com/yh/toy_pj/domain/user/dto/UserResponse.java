package com.yh.toy_pj.domain.user.dto;

import com.yh.toy_pj.domain.user.User;
import com.yh.toy_pj.domain.user.UserRole;

public record UserResponse(Long id, String name, String email, String department, UserRole role) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getDepartment(), user.getRole());
    }
}
