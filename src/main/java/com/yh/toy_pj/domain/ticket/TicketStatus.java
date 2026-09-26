package com.yh.toy_pj.domain.ticket;

import com.yh.toy_pj.global.common.CodeEnum;
import java.util.EnumSet;
import java.util.Set;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 티켓 상태와 허용되는 전이 규칙.
 *
 * <pre>
 *  OPEN ──> IN_PROGRESS ──> RESOLVED ──> CLOSED
 *   │  <──────┘  ^              │
 *   │            └──(재오픈)────┘
 *   └──> CANCELED
 * </pre>
 */
@Getter
@RequiredArgsConstructor
public enum TicketStatus implements CodeEnum {
    OPEN("접수대기"),
    IN_PROGRESS("처리중"),
    RESOLVED("해결됨"),
    CLOSED("종료"),
    CANCELED("취소");

    private final String label;

    /** 아직 처리가 끝나지 않은(대시보드/SLA 집계 대상) 상태 */
    public static final Set<TicketStatus> ACTIVE = EnumSet.of(OPEN, IN_PROGRESS);

    public Set<TicketStatus> nextStatuses() {
        return switch (this) {
            case OPEN -> EnumSet.of(IN_PROGRESS, CANCELED);
            case IN_PROGRESS -> EnumSet.of(OPEN, RESOLVED);
            case RESOLVED -> EnumSet.of(IN_PROGRESS, CLOSED);
            case CLOSED, CANCELED -> EnumSet.noneOf(TicketStatus.class);
        };
    }

    public boolean canTransitionTo(TicketStatus next) {
        return nextStatuses().contains(next);
    }

    public boolean isFinished() {
        return this == CLOSED || this == CANCELED;
    }
}
