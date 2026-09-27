package com.yh.toy_pj.support;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.yh.toy_pj.auth.jwt.AccessTokenVerifier;
import com.yh.toy_pj.auth.jwt.JwtProperties;
import com.yh.toy_pj.auth.jwt.JwtTokenProvider;
import com.yh.toy_pj.auth.oauth.OAuth2LoginFailureHandler;
import com.yh.toy_pj.auth.oauth.OAuth2LoginSuccessHandler;
import com.yh.toy_pj.domain.user.AccountStatus;
import com.yh.toy_pj.domain.user.UserRepository;
import com.yh.toy_pj.global.config.ClockConfig;
import com.yh.toy_pj.global.security.RestAccessDeniedHandler;
import com.yh.toy_pj.global.security.RestAuthenticationEntryPoint;
import com.yh.toy_pj.global.security.SecurityConfig;
import com.yh.toy_pj.global.security.SecurityErrorWriter;
import java.util.Optional;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/**
 * @WebMvcTest 는 컨트롤러 관련 빈만 올리므로, 실제와 같은 보안 규칙(JWT 필터, 401/403 처리)을 적용하기 위해 필요한 빈을 가져온다.
 */
@TestConfiguration
@EnableConfigurationProperties(JwtProperties.class)
@Import({SecurityConfig.class, JwtTokenProvider.class, AccessTokenVerifier.class, RestAuthenticationEntryPoint.class,
        RestAccessDeniedHandler.class, SecurityErrorWriter.class, ClockConfig.class})
public class WebMvcSecurityTestConfig {

    /** 웹 계층 테스트에는 DB 가 없으므로, 모든 계정을 "정상 상태(비밀번호 변경 이력 없음)"로 응답하는 가짜 저장소를 쓴다. */
    @Bean
    UserRepository userRepository() {
        UserRepository repository = mock(UserRepository.class);
        given(repository.findAccountStatusById(any())).willReturn(Optional.of(new AccountStatus(null, false)));
        return repository;
    }

    /** SNS 로그인 처리기는 웹 계층 테스트 대상이 아니므로 가짜로 채운다 (SecurityConfig 가 주입받기 때문에 빈은 있어야 함). */
    @Bean
    OAuth2LoginSuccessHandler oauth2LoginSuccessHandler() {
        return mock(OAuth2LoginSuccessHandler.class);
    }

    @Bean
    OAuth2LoginFailureHandler oauth2LoginFailureHandler() {
        return mock(OAuth2LoginFailureHandler.class);
    }
}
