package com.yh.toy_pj.global.config;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * JPA Auditing 활성화.
 * 메인 클래스가 아닌 별도 설정으로 분리해 @WebMvcTest 같은 슬라이스 테스트에서 JPA 빈 없이도 컨텍스트가 뜨도록 한다.
 *
 * 생성/수정 시각(createdAt, updatedAt)도 SLA 계산과 같은 {@link Clock}(Asia/Seoul)을 쓴다.
 * 기본 설정은 서버(JVM)의 시간대를 따르므로, UTC 로 도는 컨테이너에서는 접수 시각과 처리 기한이 9시간 어긋날 수 있다.
 */
@Configuration
@Import(ClockConfig.class)
@EnableJpaAuditing(dateTimeProviderRef = "auditingDateTimeProvider")
public class JpaConfig {

    @Bean
    public DateTimeProvider auditingDateTimeProvider(Clock clock) {
        return () -> Optional.of(LocalDateTime.now(clock));
    }
}
