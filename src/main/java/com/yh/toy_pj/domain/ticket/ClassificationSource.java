package com.yh.toy_pj.domain.ticket;

import com.yh.toy_pj.global.common.CodeEnum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 티켓의 분류/우선순위가 어떻게 결정되었는지 추적한다. */
@Getter
@RequiredArgsConstructor
public enum ClassificationSource implements CodeEnum {
    MANUAL("사용자 지정"),
    AI("AI 자동 분류"),
    RULE("키워드 규칙 분류");

    private final String label;
}
