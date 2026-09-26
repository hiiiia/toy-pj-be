package com.yh.toy_pj.domain.ticket.dto;

import com.yh.toy_pj.domain.ticket.TicketCategory;
import com.yh.toy_pj.domain.ticket.TicketPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 티켓 접수 요청. category/priority 를 비워두면 AI(또는 키워드 규칙)가 자동으로 분류한다.
 * 요청자는 본문으로 받지 않고 로그인한 사용자로 정한다. (다른 사람 이름으로 접수하는 것을 막기 위해)
 */
public record TicketCreateRequest(
        @NotBlank @Size(max = 100) String title,
        @NotBlank @Size(max = 2000) String description,
        TicketCategory category,
        TicketPriority priority,
        Long assetId
) {
}
