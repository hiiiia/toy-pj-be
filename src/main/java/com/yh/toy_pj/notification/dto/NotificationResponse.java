package com.yh.toy_pj.notification.dto;

import com.yh.toy_pj.notification.Notification;
import com.yh.toy_pj.notification.NotificationType;
import java.time.LocalDateTime;

public record NotificationResponse(Long id, NotificationType type, String message, Long ticketId, boolean read,
                                   LocalDateTime createdAt) {

    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(n.getId(), n.getType(), n.getMessage(),
                n.getTicket() != null ? n.getTicket().getId() : null, n.isRead(), n.getCreatedAt());
    }
}
