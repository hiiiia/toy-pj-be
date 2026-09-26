package com.yh.toy_pj.notification;

import com.yh.toy_pj.global.common.CodeEnum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NotificationType implements CodeEnum {
    TICKET_ASSIGNED("담당 배정", false),
    COMMENT_ADDED("새 댓글", false),
    SLA_WARNING("처리 기한 임박", true),
    SLA_BREACHED("처리 기한 초과", true);

    private final String label;

    /** 앱 내 알림 외에 외부 채널(Slack 등)로도 보낼지 여부. 긴급도가 높은 것만 보낸다. */
    private final boolean external;
}
