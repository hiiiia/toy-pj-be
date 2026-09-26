package com.yh.toy_pj.domain.ticket;

import com.yh.toy_pj.domain.asset.Asset;
import com.yh.toy_pj.domain.user.User;
import com.yh.toy_pj.global.common.BaseTimeEntity;
import com.yh.toy_pj.global.error.BusinessException;
import com.yh.toy_pj.global.error.ErrorCode;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.DynamicUpdate;

/**
 * 헬프데스크 티켓 (장애 신고/요청).
 * 상태 전이 규칙은 {@link TicketStatus} 에 정의되어 있으며, 모든 변경은 처리자와 함께 {@link TicketHistory} 로 기록된다.
 * (테이블/인덱스 정의는 Flyway 마이그레이션 파일 db/migration/V*.sql 이 기준이다.)
 */
@Entity
@DynamicUpdate // 바뀐 컬럼만 UPDATE → 스케줄러가 기록한 SLA 발송 시각을 사용자 수정이 덮어쓰지 않음
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Ticket extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TicketCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TicketPriority priority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ClassificationSource classificationSource;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TicketStatus status;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id")
    private User assignee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_id")
    private Asset asset;

    /** SLA 기반 처리 기한 */
    @Column(nullable = false)
    private LocalDateTime dueAt;

    private LocalDateTime resolvedAt;

    /** SLA 임박/초과 알림을 보낸 시각. 같은 기한에 대해 알림을 두 번 보내지 않기 위한 기록 */
    private LocalDateTime slaWarnedAt;

    private LocalDateTime slaBreachedAt;

    /** 낙관적 락: 여러 담당자가 동시에 같은 대상을 수정할 때 나중 요청이 앞선 변경을 덮어쓰지 않도록 한다. */
    @Version
    private Long version;

    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<TicketHistory> histories = new ArrayList<>();

    private Ticket(String title, String description, TicketCategory category, TicketPriority priority,
                   ClassificationSource source, User requester, Asset asset, LocalDateTime now) {
        this.title = title;
        this.description = description;
        this.category = Objects.requireNonNull(category, "category");
        this.priority = Objects.requireNonNull(priority, "priority");
        this.classificationSource = Objects.requireNonNull(source, "source");
        this.requester = Objects.requireNonNull(requester, "requester");
        this.asset = asset;
        this.status = TicketStatus.OPEN;
        this.dueAt = now.plus(priority.getSla());
        addHistory(requester, null, TicketStatus.OPEN, "티켓 접수");
    }

    public static Ticket open(String title, String description, TicketCategory category, TicketPriority priority,
                              ClassificationSource source, User requester, Asset asset, LocalDateTime now) {
        return new Ticket(title, description, category, priority, source, requester, asset, now);
    }

    public void assign(User assignee, User actor) {
        Objects.requireNonNull(assignee, "assignee");
        ensureNotFinished();
        if (!assignee.isAdmin()) {
            throw new BusinessException(ErrorCode.ASSIGNEE_NOT_ADMIN);
        }
        this.assignee = assignee;
        addHistory(actor, status, status, "담당자 지정: " + assignee.getName());
    }

    /**
     * 상태 변경. IT 관리자는 허용된 전이를 모두 할 수 있고,
     * 일반 사용자는 본인이 요청한 티켓을 "취소"하는 것만 가능하다.
     */
    public void changeStatus(TicketStatus next, String note, LocalDateTime now, User actor) {
        Objects.requireNonNull(next, "next");
        Objects.requireNonNull(actor, "actor");
        if (!actor.isAdmin() && !(isRequestedBy(actor) && next == TicketStatus.CANCELED)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "요청자는 본인 티켓의 접수 취소만 할 수 있습니다.");
        }
        if (!status.canTransitionTo(next)) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION,
                    "'%s' 상태에서 '%s' 상태로 변경할 수 없습니다.".formatted(status.getLabel(), next.getLabel()));
        }
        if (next == TicketStatus.IN_PROGRESS && assignee == null) {
            throw new BusinessException(ErrorCode.ASSIGNEE_REQUIRED);
        }
        TicketStatus previous = this.status;
        this.status = next;
        if (next == TicketStatus.RESOLVED) {
            this.resolvedAt = now;
        } else if (previous == TicketStatus.RESOLVED && next == TicketStatus.IN_PROGRESS) {
            this.resolvedAt = null; // 재오픈 (RESOLVED → CLOSED 는 해결 시각 유지)
        }
        addHistory(actor, previous, next, note);
    }

    /** 담당자가 AI/규칙 분류 결과를 수동으로 바로잡는다. 처리 기한은 접수 시각 기준으로 재계산된다. */
    public void reclassify(TicketCategory category, TicketPriority priority, LocalDateTime now, User actor) {
        ensureNotFinished();
        this.category = Objects.requireNonNull(category, "category");
        this.priority = Objects.requireNonNull(priority, "priority");
        this.classificationSource = ClassificationSource.MANUAL;
        LocalDateTime base = getCreatedAt() != null ? getCreatedAt() : now;
        this.dueAt = base.plus(priority.getSla());
        // 기한이 바뀌었으므로 새 기한 기준으로 다시 알림을 보낼 수 있게 초기화
        this.slaWarnedAt = null;
        this.slaBreachedAt = null;
        addHistory(actor, status, status, "재분류: %s / %s".formatted(category.getLabel(), priority.getLabel()));
    }

    public boolean isRequestedBy(User user) {
        return requester == user || (requester.getId() != null && requester.getId().equals(user.getId()));
    }

    public boolean isOverdue(LocalDateTime now) {
        return TicketStatus.ACTIVE.contains(status) && dueAt.isBefore(now);
    }

    public List<TicketHistory> getHistories() {
        return Collections.unmodifiableList(histories);
    }

    private void ensureNotFinished() {
        if (status.isFinished()) {
            throw new BusinessException(ErrorCode.TICKET_ALREADY_FINISHED);
        }
    }

    private void addHistory(User actor, TicketStatus from, TicketStatus to, String note) {
        histories.add(new TicketHistory(this, actor, from, to, note));
    }
}
