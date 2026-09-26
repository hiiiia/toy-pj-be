package com.yh.toy_pj.domain.ticket.dto;

import com.yh.toy_pj.domain.ticket.TicketCategory;
import com.yh.toy_pj.domain.ticket.TicketPriority;
import com.yh.toy_pj.domain.ticket.TicketStatus;

public record TicketSearchCondition(
        TicketStatus status,
        TicketPriority priority,
        TicketCategory category,
        Long requesterId,
        Long assigneeId,
        Boolean unassigned,
        String keyword
) {

    /** 요청자 조건만 강제로 바꾼 복사본 (일반 사용자 조회 범위 제한용) */
    public TicketSearchCondition withRequesterId(Long requesterId) {
        return new TicketSearchCondition(status, priority, category, requesterId, assigneeId, unassigned, keyword);
    }
}
