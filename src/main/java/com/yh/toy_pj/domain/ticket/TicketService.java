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
import com.yh.toy_pj.notification.TicketEvents;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TicketService {

    private final TicketRepository ticketRepository;
    private final UserService userService;
    private final AssetService assetService;
    private final TicketTriageService triageService;
    private final ApplicationEventPublisher eventPublisher;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;

    /**
     * 티켓 접수.
     * <ul>
     *   <li>일반 사용자는 분류(category)만 고를 수 있고, 우선순위는 항상 자동 분류로 정한다.
     *       (요청자가 "긴급"을 직접 골라 처리 기한을 앞당기지 못하게. 필요하면 담당자가 재분류한다)</li>
     *   <li>IT 관리자가 분류/우선순위를 모두 지정하면 그대로 사용한다.</li>
     *   <li>비어 있는 값은 AI(실패 시 키워드 규칙) 분류 결과로 채운다.</li>
     * </ul>
     *
     * AI 호출은 최대 수십 초 걸릴 수 있어 트랜잭션 밖에서 하고, 저장만 짧은 트랜잭션으로 처리한다.
     * (트랜잭션 안에서 기다리면 그동안 DB 커넥션을 붙잡고 있어, 동시 접수가 몰리면 커넥션 풀이 고갈된다)
     * SUPPORTS: 호출한 쪽에 트랜잭션이 없으면(컨트롤러에서 호출) 트랜잭션 없이 실행한다.
     */
    @Transactional(propagation = Propagation.SUPPORTS)
    public TicketResponse open(TicketCreateRequest request, AuthUser me) {
        User requester = userService.getUser(me.id()); // 요청자는 요청 본문이 아닌 로그인 정보로 결정
        Asset asset = request.assetId() != null ? assetService.getAsset(request.assetId()) : null;
        if (asset != null && !me.isAdmin() && !isAssignedTo(asset, me)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "본인에게 배정된 자산만 티켓에 연결할 수 있습니다.");
        }

        TicketCategory category = request.category();
        TicketPriority priority = me.isAdmin() ? request.priority() : null;
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
        return transactionTemplate.execute(status -> TicketResponse.of(ticketRepository.save(ticket), now()));
    }

    /** 일반 사용자는 검색 조건과 관계없이 본인이 요청한 티켓만 조회된다. */
    public PageResponse<TicketResponse> search(TicketSearchCondition condition, Pageable pageable, AuthUser me) {
        TicketSearchCondition scoped = me.isAdmin() ? condition : condition.withRequesterId(me.id());
        LocalDateTime now = now();
        return PageResponse.from(ticketRepository.findAll(TicketSpecs.of(scoped, now), pageable)
                .map(ticket -> TicketResponse.of(ticket, now)));
    }

    public TicketDetailResponse getDetail(Long id, AuthUser me) {
        Ticket ticket = ticketRepository.findDetailById(id).orElseThrow(() -> notFound(id));
        checkViewable(ticket, me);
        return TicketDetailResponse.of(ticket, now());
    }

    /** 조회 권한(요청자 본인 또는 관리자)을 확인하고 티켓을 돌려준다. 댓글·첨부파일 서비스에서 사용한다. */
    public Ticket getViewableTicket(Long id, AuthUser me) {
        Ticket ticket = getTicket(id);
        checkViewable(ticket, me);
        return ticket;
    }

    /** 조회 권한 + 아직 종료되지 않은 티켓인지 확인한다. (종료된 티켓에는 댓글·첨부를 추가할 수 없다) */
    public Ticket getWritableTicket(Long id, AuthUser me) {
        Ticket ticket = getViewableTicket(id, me);
        if (ticket.getStatus().isFinished()) {
            throw new BusinessException(ErrorCode.TICKET_ALREADY_FINISHED);
        }
        return ticket;
    }

    private static void checkViewable(Ticket ticket, AuthUser me) {
        if (!me.isAdmin() && !ticket.getRequester().getId().equals(me.id())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "본인이 요청한 티켓만 조회할 수 있습니다.");
        }
    }

    @Transactional
    public TicketDetailResponse assign(Long id, Long assigneeId, AuthUser me) {
        Ticket ticket = getTicket(id);
        User assignee = userService.getUser(assigneeId);
        User actor = userService.getUser(me.id());
        ticket.assign(assignee, actor);
        eventPublisher.publishEvent(new TicketEvents.Assigned(ticket, assignee, actor));
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
