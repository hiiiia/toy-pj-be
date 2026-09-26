package com.yh.toy_pj.domain.ticket;

import com.yh.toy_pj.ai.TicketTriageService;
import com.yh.toy_pj.ai.dto.TriageResult;
import com.yh.toy_pj.domain.asset.Asset;
import com.yh.toy_pj.domain.asset.AssetService;
import com.yh.toy_pj.domain.ticket.dto.TicketCreateRequest;
import com.yh.toy_pj.domain.ticket.dto.TicketDetailResponse;
import com.yh.toy_pj.domain.ticket.dto.TicketResponse;
import com.yh.toy_pj.domain.ticket.dto.TicketSearchCondition;
import com.yh.toy_pj.domain.user.User;
import com.yh.toy_pj.domain.user.UserService;
import com.yh.toy_pj.global.common.PageResponse;
import com.yh.toy_pj.global.error.BusinessException;
import com.yh.toy_pj.global.error.ErrorCode;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TicketService {

    private final TicketRepository ticketRepository;
    private final UserService userService;
    private final AssetService assetService;
    private final TicketTriageService triageService;
    private final Clock clock;

    /**
     * 티켓 접수. 분류/우선순위가 모두 지정되면 그대로 사용하고,
     * 하나라도 비어 있으면 AI(실패 시 키워드 규칙) 분류 결과로 채운다.
     */
    @Transactional
    public TicketResponse open(TicketCreateRequest request) {
        User requester = userService.getUser(request.requesterId());
        Asset asset = request.assetId() != null ? assetService.getAsset(request.assetId()) : null;

        TicketCategory category = request.category();
        TicketPriority priority = request.priority();
        ClassificationSource source = ClassificationSource.MANUAL;

        if (category == null || priority == null) {
            TriageResult triage = triageService.triage(request.title(), request.description());
            category = category != null ? category : triage.category();
            priority = priority != null ? priority : triage.priority();
            source = triage.source();
            log.info("Ticket triaged by {}: category={}, priority={}", source, category, priority);
        }

        Ticket ticket = Ticket.open(request.title(), request.description(), category, priority, source,
                requester, asset, now());
        return TicketResponse.of(ticketRepository.save(ticket), now());
    }

    public PageResponse<TicketResponse> search(TicketSearchCondition condition, Pageable pageable) {
        LocalDateTime now = now();
        return PageResponse.from(ticketRepository.findAll(TicketSpecs.of(condition), pageable)
                .map(ticket -> TicketResponse.of(ticket, now)));
    }

    public TicketDetailResponse getDetail(Long id) {
        Ticket ticket = ticketRepository.findDetailById(id).orElseThrow(() -> notFound(id));
        return TicketDetailResponse.of(ticket, now());
    }

    @Transactional
    public TicketDetailResponse assign(Long id, Long assigneeId) {
        Ticket ticket = getTicket(id);
        ticket.assign(userService.getUser(assigneeId));
        return toDetail(ticket);
    }

    @Transactional
    public TicketDetailResponse changeStatus(Long id, TicketStatus status, String note) {
        Ticket ticket = getTicket(id);
        ticket.changeStatus(status, note, now());
        return toDetail(ticket);
    }

    @Transactional
    public TicketDetailResponse reclassify(Long id, TicketCategory category, TicketPriority priority) {
        Ticket ticket = getTicket(id);
        ticket.reclassify(category, priority, now());
        return toDetail(ticket);
    }

    /**
     * 변경 직후 flush 하여 새로 추가된 처리 이력의 id/생성시각이 응답에 포함되도록 한다.
     */
    private TicketDetailResponse toDetail(Ticket ticket) {
        ticketRepository.flush();
        return TicketDetailResponse.of(ticket, now());
    }

    private Ticket getTicket(Long id) {
        return ticketRepository.findById(id).orElseThrow(() -> notFound(id));
    }

    private BusinessException notFound(Long id) {
        return new BusinessException(ErrorCode.TICKET_NOT_FOUND, "티켓을 찾을 수 없습니다. id=" + id);
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}
