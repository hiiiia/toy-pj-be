package com.yh.toy_pj.notification.dto;

import java.util.List;

public record NotificationListResponse(long unreadCount, List<NotificationResponse> items) {
}
