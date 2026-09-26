package com.yh.toy_pj.domain.ticket;

import com.yh.toy_pj.auth.AuthUser;
import com.yh.toy_pj.domain.ticket.dto.TicketAssignRequest;
import com.yh.toy_pj.domain.ticket.dto.TicketCreateRequest;
import com.yh.toy_pj.domain.ticket.dto.TicketDetailResponse;
import com.yh.toy_pj.domain.ticket.dto.TicketReclassifyRequest;
import com.yh.toy_pj.domain.ticket.dto.TicketResponse;
import com.yh.toy_pj.domain.ticket.dto.TicketSearchCondition;
import com.yh.toy_pj.domain.ticket.dto.TicketStatusChangeRequest;
import com.yh.toy_pj.global.common.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Ticket", description = "헬프데스크 티켓 (장애 신고/요청)")
@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    @Operation(summary = "티켓 접수", description = "category/priority 를 생략하면 AI(실패 시 키워드 규칙)가 자동 분류한다.")
    @PostMapping
    public ResponseEntity<TicketResponse> open(@Valid @RequestBody TicketCreateRequest request,
                                               @AuthenticationPrincipal AuthUser me) {
        TicketResponse response = ticketService.open(request, me);
        return ResponseEntity.created(URI.create("/api/tickets/" + response.id())).body(response);
    }

    @Operation(summary = "티켓 목록 검색", description = "status, priority, category, requesterId, assigneeId, unassigned, active(미완료), overdue(SLA 초과), keyword 조건과 페이징을 지원한다. 일반 사용자는 본인 티켓만 조회된다.")
    @GetMapping
    public PageResponse<TicketResponse> search(@ModelAttribute TicketSearchCondition condition,
                                               @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable,
                                               @AuthenticationPrincipal AuthUser me) {
        return ticketService.search(condition, pageable, me);
    }

    @Operation(summary = "티켓 상세 (처리 이력 포함)")
    @GetMapping("/{id}")
    public TicketDetailResponse getDetail(@PathVariable Long id, @AuthenticationPrincipal AuthUser me) {
        return ticketService.getDetail(id, me);
    }

    @Operation(summary = "담당자 지정", description = "IT 관리자(ADMIN)만 담당자로 지정할 수 있다.")
    @PostMapping("/{id}/assign")
    public TicketDetailResponse assign(@PathVariable Long id, @Valid @RequestBody TicketAssignRequest request,
                                       @AuthenticationPrincipal AuthUser me) {
        return ticketService.assign(id, request.assigneeId(), me);
    }

    @Operation(summary = "상태 변경", description = "허용된 전이만 가능: OPEN→IN_PROGRESS/CANCELED, IN_PROGRESS→OPEN/RESOLVED, RESOLVED→IN_PROGRESS/CLOSED. 일반 사용자는 본인 티켓 취소만 가능.")
    @PatchMapping("/{id}/status")
    public TicketDetailResponse changeStatus(@PathVariable Long id, @Valid @RequestBody TicketStatusChangeRequest request,
                                             @AuthenticationPrincipal AuthUser me) {
        return ticketService.changeStatus(id, request.status(), request.note(), me);
    }

    @Operation(summary = "재분류", description = "자동 분류 결과를 담당자가 수정한다. 처리 기한(SLA)이 재계산된다.")
    @PatchMapping("/{id}/classification")
    public TicketDetailResponse reclassify(@PathVariable Long id, @Valid @RequestBody TicketReclassifyRequest request,
                                           @AuthenticationPrincipal AuthUser me) {
        return ticketService.reclassify(id, request.category(), request.priority(), me);
    }
}
