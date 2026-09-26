package com.yh.toy_pj.support;

import com.yh.toy_pj.auth.jwt.JwtProperties;
import com.yh.toy_pj.auth.jwt.JwtTokenProvider;
import com.yh.toy_pj.global.config.ClockConfig;
import com.yh.toy_pj.global.security.RestAccessDeniedHandler;
import com.yh.toy_pj.global.security.RestAuthenticationEntryPoint;
import com.yh.toy_pj.global.security.SecurityConfig;
import com.yh.toy_pj.global.security.SecurityErrorWriter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;

/**
 * @WebMvcTest 는 컨트롤러 관련 빈만 올리므로, 실제와 같은 보안 규칙(JWT 필터, 401/403 처리)을 적용하기 위해 필요한 빈을 가져온다.
 */
@TestConfiguration
@EnableConfigurationProperties(JwtProperties.class)
@Import({SecurityConfig.class, JwtTokenProvider.class, RestAuthenticationEntryPoint.class,
        RestAccessDeniedHandler.class, SecurityErrorWriter.class, ClockConfig.class})
public class WebMvcSecurityTestConfig {
}
