package com.yh.toy_pj.domain.ticket.dto;

import com.yh.toy_pj.domain.ticket.TicketCategory;
import com.yh.toy_pj.domain.ticket.TicketPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 티켓 접수 요청. category/priority 를 비워두면 AI(또는 키워드 규칙)가 자동으로 분류한다.
 */
public record TicketCreateRequest(
        @NotBlank @Size(max = 100) String title,
        @NotBlank @Size(max = 2000) String description,
        TicketCategory category,
        TicketPriority priority,
        @NotNull Long requesterId,
        Long assetId
) {
}
