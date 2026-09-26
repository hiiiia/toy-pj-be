package com.yh.toy_pj.domain.ticket.comment;

import com.yh.toy_pj.domain.ticket.Ticket;
import com.yh.toy_pj.domain.user.User;
import com.yh.toy_pj.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 티켓 댓글. 요청자와 담당자가 추가 정보를 주고받는 용도.
 * internal = true 인 "내부 메모"는 IT 관리자에게만 보인다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TicketComment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(nullable = false, length = 2000)
    private String content;

    @Column(nullable = false)
    private boolean internal;

    private TicketComment(Ticket ticket, User author, String content, boolean internal) {
        this.ticket = Objects.requireNonNull(ticket);
        this.author = Objects.requireNonNull(author);
        this.content = content;
        this.internal = internal;
    }

    public static TicketComment write(Ticket ticket, User author, String content, boolean internal) {
        return new TicketComment(ticket, author, content, internal);
    }

    public boolean isWrittenBy(Long userId) {
        return author.getId().equals(userId);
    }
}
