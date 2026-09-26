package com.yh.toy_pj.domain.ticket.dto;

import com.yh.toy_pj.domain.ticket.TicketCategory;
import com.yh.toy_pj.domain.ticket.TicketPriority;
import jakarta.validation.constraints.NotNull;

public record TicketReclassifyRequest(@NotNull TicketCategory category, @NotNull TicketPriority priority) {
}
