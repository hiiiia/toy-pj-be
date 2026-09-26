package com.yh.toy_pj.domain.dashboard;

import com.yh.toy_pj.domain.asset.AssetStatus;
import com.yh.toy_pj.domain.ticket.TicketPriority;
import com.yh.toy_pj.domain.ticket.TicketStatus;
import java.util.Map;

public record DashboardResponse(
        TicketSummary tickets,
        AssetSummary assets
) {

    public record TicketSummary(
            long total,
            Map<TicketStatus, Long> byStatus,
            Map<TicketPriority, Long> activeByPriority,
            long unassigned,
            long overdue
    ) {
    }

    public record AssetSummary(
            long total,
            Map<AssetStatus, Long> byStatus
    ) {
    }
}
