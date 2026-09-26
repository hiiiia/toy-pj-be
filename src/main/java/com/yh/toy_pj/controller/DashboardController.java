package com.yh.toy_pj.controller;

import com.yh.toy_pj.repository.AssetRepository;
import com.yh.toy_pj.repository.TicketRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class DashboardController {

    @Autowired
    private AssetRepository assetRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @GetMapping("/api/dashboard/stats")
    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();

        // 자산 관련 통계
        stats.put("totalAssets", assetRepository.count());
        stats.put("inUseAssets", assetRepository.countByStatus("사용중"));

        // 티켓 관련 통계
        stats.put("totalTickets", ticketRepository.count());
        stats.put("pendingTickets", ticketRepository.countByStatus("접수대기"));

        return stats;
    }
}