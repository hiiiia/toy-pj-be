package com.yh.toy_pj.notification;

import com.yh.toy_pj.domain.ticket.Ticket;
import com.yh.toy_pj.domain.user.User;
import com.yh.toy_pj.domain.user.UserRepository;
import com.yh.toy_pj.domain.user.UserRole;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 티켓 이벤트 → 누구에게 알릴지 결정한다.
 *
 * {@code @EventListener} 는 이벤트를 발행한 트랜잭션 안에서 바로 실행된다.
 * 그래서 댓글 저장과 알림 저장이 함께 커밋되거나 함께 롤백된다. (댓글은 저장됐는데 알림만 사라지는 일이 없음)
 */
@Component
@RequiredArgsConstructor
public class TicketNotificationListener {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    @EventListener
    public void onAssigned(TicketEvents.Assigned event) {
        if (event.assignee().getId().equals(event.actor().getId())) {
            return; // 스스로 담당을 맡은 경우는 알리지 않는다
        }
        Ticket ticket = event.ticket();
        notificationService.notify(List.of(event.assignee()), ticket, NotificationType.TICKET_ASSIGNED,
                "%s님이 #%d '%s' 티켓을 배정했습니다.".formatted(event.actor().getName(), ticket.getId(), ticket.getTitle()));
    }

    /**
     * 댓글 알림 대상
     * - 요청자가 쓴 댓글 → 담당자 (담당자가 없으면 IT 관리자 전체)
     * - 관리자가 쓴 일반 댓글 → 요청자 + 담당자
     * - 내부 메모 → 담당자 (요청자에게는 절대 알리지 않음)
     * 작성자 본인은 제외한다.
     */
    @EventListener
    public void onCommentAdded(TicketEvents.CommentAdded event) {
        Ticket ticket = event.ticket();
        User author = event.author();
        List<User> recipients = new ArrayList<>();

        if (ticket.isRequestedBy(author)) {
            if (ticket.getAssignee() != null) {
                recipients.add(ticket.getAssignee());
            } else {
                recipients.addAll(userRepository.findByRole(UserRole.ADMIN));
            }
        } else {
            if (!event.internal()) {
                recipients.add(ticket.getRequester());
            }
            if (ticket.getAssignee() != null) {
                recipients.add(ticket.getAssignee());
            }
        }
        recipients.removeIf(user -> user.getId().equals(author.getId()));
        if (recipients.isEmpty()) {
            return;
        }
        String prefix = event.internal() ? "내부 메모" : "새 댓글";
        notificationService.notify(recipients, ticket, NotificationType.COMMENT_ADDED,
                "[%s] %s님이 #%d '%s'에 댓글을 남겼습니다.".formatted(prefix, author.getName(), ticket.getId(), ticket.getTitle()));
    }
}
