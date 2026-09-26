package com.yh.toy_pj.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * 테스트용 PostgreSQL 컨테이너.
 *
 * H2 는 PostgreSQL 과 문법·동작이 미묘하게 달라 "테스트는 통과했는데 운영에서 실패"하는 일이 생길 수 있다.
 * 그래서 운영과 같은 PostgreSQL 16 을 Docker 로 띄워 테스트한다.
 *
 * - 컨테이너는 JVM 당 한 번만 띄워 모든 테스트가 공유한다. (static 필드 + 즉시 시작)
 * - @ServiceConnection: 컨테이너의 주소/계정을 spring.datasource.* 에 자동으로 연결한다.
 * - 테스트가 끝나면 Testcontainers 가 컨테이너를 자동으로 정리한다.
 */
@TestConfiguration(proxyBeanMethods = false)
public class PostgresTestContainer {

    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");

    static {
        POSTGRES.start();
    }

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
        return POSTGRES;
    }
}
