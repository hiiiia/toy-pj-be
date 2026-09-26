package com.yh.toy_pj.domain.dashboard;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Dashboard", description = "운영 현황 통계")
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @Operation(summary = "대시보드 요약", description = "티켓 상태/우선순위별 건수, 미배정·SLA 초과 건수, 자산 상태별 건수")
    @GetMapping("/summary")
    public DashboardResponse getSummary() {
        return dashboardService.getSummary();
    }
}
