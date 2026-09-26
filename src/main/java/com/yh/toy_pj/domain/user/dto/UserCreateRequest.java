package com.yh.toy_pj.domain.user.dto;

import com.yh.toy_pj.domain.user.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UserCreateRequest(
        @NotBlank @Size(max = 50) String name,
        @NotBlank @Email @Size(max = 100) String email,
        @Size(max = 50) String department,
        @NotNull UserRole role
) {
}
