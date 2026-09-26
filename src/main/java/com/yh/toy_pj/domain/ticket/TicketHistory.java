package com.yh.toy_pj.domain.ticket;

import com.yh.toy_pj.domain.user.User;
import com.yh.toy_pj.global.common.BaseTimeEntity;
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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 티켓 처리 이력(상태 변경, 담당자 지정, 재분류). 누가 언제 무엇을 했는지 추적하기 위한 감사 로그.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TicketHistory extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    /** 처리자: 누가 이 변경을 했는지 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id")
    private User actor;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private TicketStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TicketStatus toStatus;

    @Column(length = 500)
    private String note;

    TicketHistory(Ticket ticket, User actor, TicketStatus fromStatus, TicketStatus toStatus, String note) {
        this.ticket = ticket;
        this.actor = actor;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.note = note;
    }
}
