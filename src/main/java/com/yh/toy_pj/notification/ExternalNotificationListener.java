package com.yh.toy_pj.notification;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 외부 채널 발송은 트랜잭션이 "커밋된 뒤"에만 한다.
 * 커밋 전에 보내면, 이후 DB 오류로 롤백됐을 때 이미 나간 Slack 메시지를 되돌릴 수 없기 때문이다.
 */
@Component
@RequiredArgsConstructor
public class ExternalNotificationListener {

    private final SlackNotifier slackNotifier;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onExternalNotification(NotificationService.ExternalNotification event) {
        slackNotifier.send("[%s] %s".formatted(event.type().getLabel(), event.message()));
    }
}
