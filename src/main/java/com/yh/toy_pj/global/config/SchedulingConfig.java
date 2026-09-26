package com.yh.toy_pj.global.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 주기 작업(SLA 점검, 만료 토큰/알림 정리) 활성화.
 * 테스트에서는 app.scheduling.enabled=false 로 꺼서, 테스트 도중 작업이 끼어들어 결과가 흔들리지 않게 한다.
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "app.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class SchedulingConfig {
}
