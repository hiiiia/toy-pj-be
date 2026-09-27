package com.yh.toy_pj.domain.user;

import com.yh.toy_pj.global.common.CodeEnum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SocialProvider implements CodeEnum {
    GOOGLE("구글"),
    KAKAO("카카오"),
    NAVER("네이버");          // ← 값 뒤에 필드/메서드가 오면 마지막에 세미콜론 필수

    private final String label;
}