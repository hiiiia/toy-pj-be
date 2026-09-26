package com.yh.toy_pj.notification;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 주기 작업. 서버가 1대라는 전제이다.
 * 서버를 여러 대로 늘리면 같은 작업이 동시에 실행되므로 ShedLock 같은 분산 락이 필요하다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationScheduler {

    private final SlaMonitor slaMonitor;
    private final NotificationRepository notificationRepository;
    private final NotificationProperties properties;
    private final Clock clock;

    /** 이전 실행이 끝난 뒤 5분 후 다시 실행 (fixedDelay → 실행이 길어져도 겹치지 않음) */
    @Scheduled(fixedDelayString = "${app.notification.sla-check-interval:PT5M}", initialDelayString = "PT30S")
    public void checkSla() {
        withJobId("sla", slaMonitor::check);
    }

    @Scheduled(cron = "0 30 4 * * *", zone = "Asia/Seoul")
    @Transactional
    public void deleteOldReadNotifications() {
        withJobId("cleanup", () -> {
            int deleted = notificationRepository.deleteReadBefore(LocalDateTime.now(clock).minus(properties.readRetention()));
            log.info("오래된 읽은 알림 {}건 삭제", deleted);
        });
    }

    /** 스케줄 작업도 요청처럼 추적 ID 를 붙여 로그를 묶어 볼 수 있게 한다. */
    private static void withJobId(String job, Runnable task) {
        MDC.put("requestId", job + "-" + UUID.randomUUID().toString().substring(0, 8));
        try {
            task.run();
        } catch (RuntimeException e) {
            log.error("스케줄 작업 실패: {}", job, e);
        } finally {
            MDC.remove("requestId");
        }
    }
}
