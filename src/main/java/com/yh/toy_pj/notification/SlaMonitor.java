package com.yh.toy_pj.notification;

import com.yh.toy_pj.domain.ticket.Ticket;
import com.yh.toy_pj.domain.ticket.TicketRepository;
import com.yh.toy_pj.domain.ticket.TicketStatus;
import com.yh.toy_pj.domain.user.User;
import com.yh.toy_pj.domain.user.UserRepository;
import com.yh.toy_pj.domain.user.UserRole;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * SLA(처리 기한) 점검. 스케줄러가 주기적으로 호출한다.
 *
 * <ul>
 *   <li>기한 초과: 기한이 지난 진행중 티켓 → "처리 기한 초과" 알림</li>
 *   <li>기한 임박: 기한까지 {@code slaWarningBefore} 이내로 남은 티켓 → "처리 기한 임박" 알림</li>
 *   <li>받는 사람: 담당자. 담당자가 없으면 IT 관리자 전체</li>
 *   <li>티켓마다 발송 시각을 기록해 같은 알림을 반복해서 보내지 않는다. (재분류로 기한이 바뀌면 다시 보냄)</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SlaMonitor {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("MM.dd HH:mm");

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final NotificationProperties properties;
    private final Clock clock;

    public record Result(int warned, int breached) {
    }

    @Transactional
    public Result check() {
        LocalDateTime now = LocalDateTime.now(clock);
        List<User> admins = userRepository.findByRole(UserRole.ADMIN);

        List<Ticket> breached = ticketRepository.findSlaBreachTargets(TicketStatus.ACTIVE, now);
        breached.forEach(t -> notificationService.notify(recipients(t, admins), t, NotificationType.SLA_BREACHED,
                "#%d '%s' 처리 기한(%s)이 지났습니다. [%s]".formatted(
                        t.getId(), t.getTitle(), t.getDueAt().format(TIME), t.getPriority().getLabel())));
        if (!breached.isEmpty()) {
            ticketRepository.markSlaBreached(breached.stream().map(Ticket::getId).toList(), now);
        }

        List<Ticket> warnings = ticketRepository.findSlaWarningTargets(
                TicketStatus.ACTIVE, now, now.plus(properties.slaWarningBefore()));
        warnings.forEach(t -> notificationService.notify(recipients(t, admins), t, NotificationType.SLA_WARNING,
                "#%d '%s' 처리 기한까지 %s 남았습니다. (%s) [%s]".formatted(
                        t.getId(), t.getTitle(), humanize(Duration.between(now, t.getDueAt())),
                        t.getDueAt().format(TIME), t.getPriority().getLabel())));
        if (!warnings.isEmpty()) {
            ticketRepository.markSlaWarned(warnings.stream().map(Ticket::getId).toList(), now);
        }

        if (!breached.isEmpty() || !warnings.isEmpty()) {
            log.info("SLA 점검: 임박 {}건, 초과 {}건 알림", warnings.size(), breached.size());
        }
        return new Result(warnings.size(), breached.size());
    }

    /** 95분 → "1시간 35분", 40분 → "40분" */
    static String humanize(Duration remaining) {
        long minutes = Math.max(1, remaining.toMinutes());
        long hours = minutes / 60;
        long rest = minutes % 60;
        if (hours == 0) {
            return rest + "분";
        }
        return rest == 0 ? hours + "시간" : hours + "시간 " + rest + "분";
    }

    private static List<User> recipients(Ticket ticket, List<User> admins) {
        return ticket.getAssignee() != null ? List.of(ticket.getAssignee()) : admins;
    }
}
