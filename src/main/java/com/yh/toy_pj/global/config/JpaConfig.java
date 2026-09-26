package com.yh.toy_pj.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * JPA Auditing 활성화.
 * 메인 클래스가 아닌 별도 설정으로 분리해 @WebMvcTest 같은 슬라이스 테스트에서 JPA 빈 없이도 컨텍스트가 뜨도록 한다.
 */
@Configuration
@EnableJpaAuditing
public class JpaConfig {
}
