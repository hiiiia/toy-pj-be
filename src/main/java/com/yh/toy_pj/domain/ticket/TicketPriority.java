package com.yh.toy_pj.domain.ticket;

import com.yh.toy_pj.global.common.CodeEnum;
import java.time.Duration;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 우선순위별 처리 기한(SLA). 티켓 생성 시 이 값으로 처리 기한(dueAt)이 계산된다.
 */
@Getter
@RequiredArgsConstructor
public enum TicketPriority implements CodeEnum {
    LOW("낮음", Duration.ofHours(72)),
    MEDIUM("보통", Duration.ofHours(24)),
    HIGH("높음", Duration.ofHours(8)),
    URGENT("긴급", Duration.ofHours(4));

    private final String label;
    private final Duration sla;
}
