package com.yh.toy_pj.notification;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * @param slackWebhookUrl Slack Incoming Webhook 주소. 비어 있으면 외부 알림은 로그로만 남긴다.
 * @param slaWarningBefore 처리 기한 몇 분 전에 "임박" 알림을 보낼지
 * @param readRetention    읽은 알림 보관 기간 (지나면 삭제)
 */
@ConfigurationProperties(prefix = "app.notification")
public record NotificationProperties(
        String slackWebhookUrl,
        @DefaultValue("1h") Duration slaWarningBefore,
        @DefaultValue("30d") Duration readRetention
) {
}
