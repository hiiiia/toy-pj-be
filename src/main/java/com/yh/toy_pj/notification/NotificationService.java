package com.yh.toy_pj.notification;

import com.yh.toy_pj.auth.AuthUser;
import com.yh.toy_pj.domain.ticket.Ticket;
import com.yh.toy_pj.domain.user.User;
import com.yh.toy_pj.global.error.BusinessException;
import com.yh.toy_pj.global.error.ErrorCode;
import com.yh.toy_pj.notification.dto.NotificationListResponse;
import com.yh.toy_pj.notification.dto.NotificationResponse;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private static final int RECENT_LIMIT = 30;

    private final NotificationRepository notificationRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    /** 외부 채널(Slack 등)로 보낼 알림. 트랜잭션이 커밋된 뒤에만 발송된다. ({@link ExternalNotificationListener}) */
    public record ExternalNotification(NotificationType type, String message) {
    }

    /**
     * 앱 내 알림을 저장하고, 긴급 알림은 외부 채널로도 보낸다.
     * 같은 사람이 목록에 여러 번 있어도 한 번만 저장한다.
     */
    @Transactional
    public void notify(Collection<User> recipients, Ticket ticket, NotificationType type, String message) {
        Map<Long, User> unique = new LinkedHashMap<>();
        recipients.forEach(user -> unique.putIfAbsent(user.getId(), user));
        unique.values().forEach(user -> notificationRepository.save(new Notification(user, ticket, type, message)));
        if (type.isExternal() && !unique.isEmpty()) {
            eventPublisher.publishEvent(new ExternalNotification(type, message));
        }
    }

    public NotificationListResponse findRecent(AuthUser me) {
        var items = notificationRepository.findRecent(me.id(), PageRequest.of(0, RECENT_LIMIT)).stream()
                .map(NotificationResponse::from)
                .toList();
        return new NotificationListResponse(notificationRepository.countByRecipientIdAndReadAtIsNull(me.id()), items);
    }

    @Transactional
    public void markRead(Long id, AuthUser me) {
        notificationRepository.findByIdAndRecipientId(id, me.id())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "알림을 찾을 수 없습니다."))
                .markRead(LocalDateTime.now(clock));
    }

    @Transactional
    public void markAllRead(AuthUser me) {
        notificationRepository.markAllRead(me.id(), LocalDateTime.now(clock));
    }
}
