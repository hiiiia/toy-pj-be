package com.yh.toy_pj.domain.ticket.dto;

import com.yh.toy_pj.domain.ticket.TicketStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TicketStatusChangeRequest(@NotNull TicketStatus status, @Size(max = 500) String note) {
}
