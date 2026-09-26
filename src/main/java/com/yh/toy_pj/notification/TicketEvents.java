package com.yh.toy_pj.notification;

import com.yh.toy_pj.domain.ticket.Ticket;
import com.yh.toy_pj.domain.user.User;

/**
 * 티켓 도메인에서 발생하는 이벤트.
 * 티켓/댓글 서비스는 "이런 일이 일어났다"만 알리고, 누구에게 어떻게 알릴지는 {@link TicketNotificationListener} 가 정한다.
 * → 알림 규칙이 바뀌어도 티켓 서비스 코드는 수정할 필요가 없다.
 */
public final class TicketEvents {

    private TicketEvents() {
    }

    public record Assigned(Ticket ticket, User assignee, User actor) {
    }

    public record CommentAdded(Ticket ticket, User author, boolean internal) {
    }
}
