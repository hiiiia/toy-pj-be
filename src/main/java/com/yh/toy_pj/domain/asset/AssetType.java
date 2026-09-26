package com.yh.toy_pj.domain.asset;

import com.yh.toy_pj.global.common.CodeEnum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AssetType implements CodeEnum {
    LAPTOP("노트북"),
    DESKTOP("데스크탑"),
    MONITOR("모니터"),
    MOBILE("모바일 기기"),
    SOFTWARE("소프트웨어 라이선스"),
    ETC("기타");

    private final String label;
}
