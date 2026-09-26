package com.yh.toy_pj.domain.ticket.dto;

import jakarta.validation.constraints.NotNull;

public record TicketAssignRequest(@NotNull Long assigneeId) {
}
