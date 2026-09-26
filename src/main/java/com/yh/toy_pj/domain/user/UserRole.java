package com.yh.toy_pj.domain.user;

import com.yh.toy_pj.global.common.CodeEnum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserRole implements CodeEnum {
    USER("일반 사용자"),
    ADMIN("IT 관리자");

    private final String label;
}
