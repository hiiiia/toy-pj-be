package com.yh.toy_pj.auth;

import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** 만료된 refresh token 을 매일 새벽 정리해 테이블이 계속 커지지 않도록 한다. */
@Slf4j
@Component
@RequiredArgsConstructor
public class RefreshTokenCleaner {

    private final RefreshTokenRepository refreshTokenRepository;
    private final Clock clock;

    @Scheduled(cron = "0 0 4 * * *", zone = "Asia/Seoul")
    @Transactional
    public void deleteExpired() {
        int deleted = refreshTokenRepository.deleteExpired(LocalDateTime.now(clock));
        log.info("만료된 refresh token {}건 삭제", deleted);
    }
}
