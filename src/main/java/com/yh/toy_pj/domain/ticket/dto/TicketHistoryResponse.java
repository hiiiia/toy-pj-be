package com.yh.toy_pj.domain.ticket.dto;

import com.yh.toy_pj.domain.ticket.TicketHistory;
import com.yh.toy_pj.domain.ticket.TicketStatus;
import java.time.LocalDateTime;

public record TicketHistoryResponse(Long id, TicketStatus fromStatus, TicketStatus toStatus, String note,
                                    String actorName, LocalDateTime createdAt) {

    public static TicketHistoryResponse from(TicketHistory history) {
        return new TicketHistoryResponse(history.getId(), history.getFromStatus(), history.getToStatus(),
                history.getNote(), history.getActor() != null ? history.getActor().getName() : null,
                history.getCreatedAt());
    }
}
