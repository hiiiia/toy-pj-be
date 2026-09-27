package com.yh.toy_pj.domain.user;

import com.yh.toy_pj.global.common.CodeEnum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SocialProvider implements CodeEnum {
    GOOGLE("구글"),
    KAKAO("카카오"),
    NAVER("네이버");

    private final String label;
}