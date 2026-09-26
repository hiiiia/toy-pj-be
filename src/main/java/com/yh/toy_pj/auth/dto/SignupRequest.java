package com.yh.toy_pj.auth.dto;

import com.yh.toy_pj.auth.PasswordPolicy;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** 회원가입은 항상 일반 사용자(USER)로 가입된다. 관리자는 관리자만 등록할 수 있다. */
public record SignupRequest(
        @NotBlank @Size(max = 50) String name,
        @NotBlank @Email @Size(max = 100) String email,
        @NotBlank @Pattern(regexp = PasswordPolicy.REGEX, message = PasswordPolicy.MESSAGE) String password,
        @Size(max = 50) String department
) {
}
