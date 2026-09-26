package com.yh.toy_pj.domain.asset;

import com.yh.toy_pj.global.common.CodeEnum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AssetStatus implements CodeEnum {
    AVAILABLE("재고"),
    IN_USE("사용중"),
    REPAIR("점검중"),
    DISPOSED("폐기");

    private final String label;
}
