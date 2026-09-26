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
}
