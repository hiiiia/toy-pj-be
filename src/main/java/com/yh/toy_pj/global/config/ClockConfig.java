package com.yh.toy_pj.global.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 현재 시각을 Clock 빈으로 주입받아 SLA/지연 계산 로직을 테스트에서 고정된 시각으로 검증할 수 있게 한다.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.system(ZoneId.of("Asia/Seoul"));
    }
}
