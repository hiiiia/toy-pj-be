package com.yh.toy_pj.ai.dto;

import com.yh.toy_pj.domain.ticket.ClassificationSource;
import com.yh.toy_pj.domain.ticket.TicketCategory;
import com.yh.toy_pj.domain.ticket.TicketPriority;

public record TriageResult(TicketCategory category, TicketPriority priority, ClassificationSource source, String reason) {
}
