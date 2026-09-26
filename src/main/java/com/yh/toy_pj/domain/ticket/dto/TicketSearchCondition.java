package com.yh.toy_pj.domain.ticket.dto;

import com.yh.toy_pj.domain.ticket.TicketCategory;
import com.yh.toy_pj.domain.ticket.TicketPriority;
import com.yh.toy_pj.domain.ticket.TicketStatus;

/**
 * @param active  true 면 아직 끝나지 않은 티켓(접수대기·처리중)만
 * @param overdue true 면 처리 기한이 지난 진행중 티켓만 (SLA 초과)
 */
public record TicketSearchCondition(
        TicketStatus status,
        TicketPriority priority,
        TicketCategory category,
        Long requesterId,
        Long assigneeId,
        Boolean unassigned,
        String keyword,
        Boolean active,
        Boolean overdue
) {

    /** 요청자 조건만 강제로 바꾼 복사본 (일반 사용자 조회 범위 제한용) */
    public TicketSearchCondition withRequesterId(Long requesterId) {
        return new TicketSearchCondition(status, priority, category, requesterId, assigneeId, unassigned, keyword,
                active, overdue);
    }
}
