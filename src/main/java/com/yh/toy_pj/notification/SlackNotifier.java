package com.yh.toy_pj.notification;

import java.time.Duration;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Slack Incoming Webhook 발송. 주소가 없으면 로그로만 남긴다.
 * 알림 발송 실패가 본 업무(티켓 처리, SLA 점검)를 실패시키면 안 되므로 예외를 밖으로 던지지 않는다.
 */
@Slf4j
@Component
public class SlackNotifier {

    private final String webhookUrl;
    private final RestClient restClient;

    public SlackNotifier(NotificationProperties properties) {
        this.webhookUrl = properties.slackWebhookUrl();
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(5));
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    public boolean isEnabled() {
        return StringUtils.hasText(webhookUrl);
    }

    public void send(String text) {
        if (!isEnabled()) {
            log.info("[외부 알림 - Slack 미설정] {}", text);
            return;
        }
        try {
            restClient.post().uri(webhookUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("text", text))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            log.warn("Slack 알림 발송 실패: {}", e.getMessage());
        }
    }
}
