package com.yh.toy_pj.domain.ticket;

import com.yh.toy_pj.ai.TicketTriageService;
import com.yh.toy_pj.auth.AuthUser;
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
    public TicketResponse open(TicketCreateRequest request, AuthUser me) {
        User requester = userService.getUser(me.id()); // 요청자는 요청 본문이 아닌 로그인 정보로 결정
        Asset asset = request.assetId() != null ? assetService.getAsset(request.assetId()) : null;
        if (asset != null && !me.isAdmin() && !isAssignedTo(asset, me)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "본인에게 배정된 자산만 티켓에 연결할 수 있습니다.");
        }

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

    /** 일반 사용자는 검색 조건과 관계없이 본인이 요청한 티켓만 조회된다. */
    public PageResponse<TicketResponse> search(TicketSearchCondition condition, Pageable pageable, AuthUser me) {
        TicketSearchCondition scoped = me.isAdmin() ? condition : condition.withRequesterId(me.id());
        LocalDateTime now = now();
        return PageResponse.from(ticketRepository.findAll(TicketSpecs.of(scoped), pageable)
                .map(ticket -> TicketResponse.of(ticket, now)));
    }

    public TicketDetailResponse getDetail(Long id, AuthUser me) {
        Ticket ticket = ticketRepository.findDetailById(id).orElseThrow(() -> notFound(id));
        if (!me.isAdmin() && !ticket.getRequester().getId().equals(me.id())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "본인이 요청한 티켓만 조회할 수 있습니다.");
        }
        return TicketDetailResponse.of(ticket, now());
    }

    @Transactional
    public TicketDetailResponse assign(Long id, Long assigneeId, AuthUser me) {
        Ticket ticket = getTicket(id);
        ticket.assign(userService.getUser(assigneeId), userService.getUser(me.id()));
        return toDetail(ticket);
    }

    @Transactional
    public TicketDetailResponse changeStatus(Long id, TicketStatus status, String note, AuthUser me) {
        Ticket ticket = getTicket(id);
        ticket.changeStatus(status, note, now(), userService.getUser(me.id()));
        return toDetail(ticket);
    }

    @Transactional
    public TicketDetailResponse reclassify(Long id, TicketCategory category, TicketPriority priority, AuthUser me) {
        Ticket ticket = getTicket(id);
        ticket.reclassify(category, priority, now(), userService.getUser(me.id()));
        return toDetail(ticket);
    }

    /**
     * 변경 직후 flush 하여 새로 추가된 처리 이력의 id/생성시각이 응답에 포함되도록 한다.
     */
    private TicketDetailResponse toDetail(Ticket ticket) {
        ticketRepository.flush();
        return TicketDetailResponse.of(ticket, now());
    }

    private static boolean isAssignedTo(Asset asset, AuthUser me) {
        return asset.getAssignedUser() != null && asset.getAssignedUser().getId().equals(me.id());
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
