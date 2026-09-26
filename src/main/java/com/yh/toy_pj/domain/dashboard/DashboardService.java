package com.yh.toy_pj.domain.dashboard;

import com.yh.toy_pj.domain.asset.AssetRepository;
import com.yh.toy_pj.domain.asset.AssetStatus;
import com.yh.toy_pj.domain.dashboard.DashboardResponse.AssetSummary;
import com.yh.toy_pj.domain.dashboard.DashboardResponse.TicketSummary;
import com.yh.toy_pj.domain.ticket.TicketPriority;
import com.yh.toy_pj.domain.ticket.TicketRepository;
import com.yh.toy_pj.domain.ticket.TicketStatus;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 대시보드 통계. 엔티티를 전부 불러와 세지 않고 DB 의 GROUP BY 집계 결과만 조회한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final TicketRepository ticketRepository;
    private final AssetRepository assetRepository;
    private final Clock clock;

    public DashboardResponse getSummary() {
        return new DashboardResponse(ticketSummary(), assetSummary());
    }

    private TicketSummary ticketSummary() {
        Map<TicketStatus, Long> byStatus = zeroFilled(TicketStatus.class);
        ticketRepository.countGroupByStatus().forEach(c -> byStatus.put(c.getCode(), c.getTotal()));

        Map<TicketPriority, Long> byPriority = zeroFilled(TicketPriority.class);
        ticketRepository.countGroupByPriority(TicketStatus.ACTIVE).forEach(c -> byPriority.put(c.getCode(), c.getTotal()));

        long total = byStatus.values().stream().mapToLong(Long::longValue).sum();
        long unassigned = ticketRepository.countByAssigneeIsNullAndStatus(TicketStatus.OPEN);
        long overdue = ticketRepository.countOverdue(TicketStatus.ACTIVE, LocalDateTime.now(clock));
        return new TicketSummary(total, byStatus, byPriority, unassigned, overdue);
    }

    private AssetSummary assetSummary() {
        Map<AssetStatus, Long> byStatus = zeroFilled(AssetStatus.class);
        assetRepository.countGroupByStatus().forEach(c -> byStatus.put(c.getStatus(), c.getTotal()));
        long total = byStatus.values().stream().mapToLong(Long::longValue).sum();
        return new AssetSummary(total, byStatus);
    }

    /** 데이터가 없는 항목도 0 으로 내려줘 프론트엔드에서 null 처리를 하지 않아도 되게 한다. */
    private static <E extends Enum<E>> Map<E, Long> zeroFilled(Class<E> type) {
        Map<E, Long> map = new EnumMap<>(type);
        Arrays.stream(type.getEnumConstants()).forEach(e -> map.put(e, 0L));
        return map;
    }
}
