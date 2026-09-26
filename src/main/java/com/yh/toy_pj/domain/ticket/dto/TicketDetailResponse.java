package com.yh.toy_pj.domain.ticket.dto;

import com.yh.toy_pj.domain.ticket.Ticket;
import com.yh.toy_pj.domain.ticket.TicketStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * 티켓 상세. 화면에서 가능한 다음 상태 버튼을 그릴 수 있도록 nextStatuses 를 함께 내려준다.
 */
public record TicketDetailResponse(
        TicketResponse ticket,
        Set<TicketStatus> nextStatuses,
        List<TicketHistoryResponse> histories
) {

    public static TicketDetailResponse of(Ticket ticket, LocalDateTime now) {
        return new TicketDetailResponse(
                TicketResponse.of(ticket, now),
                ticket.getStatus().nextStatuses(),
                ticket.getHistories().stream().map(TicketHistoryResponse::from).toList()
        );
    }
}
