package com.yh.toy_pj.auth;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * 로그인 정책.
 *
 * @param maxLoginAttempts 연속 실패 허용 횟수 (도달하면 잠금)
 * @param lockDuration     잠금 유지 시간
 */
@ConfigurationProperties(prefix = "app.auth")
public record AuthPolicyProperties(
        @DefaultValue("5") int maxLoginAttempts,
        @DefaultValue("15m") Duration lockDuration
) {
}
