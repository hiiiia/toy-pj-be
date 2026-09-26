package com.yh.toy_pj.global.security;

import com.yh.toy_pj.auth.jwt.JwtAuthenticationFilter;
import com.yh.toy_pj.auth.jwt.JwtTokenProvider;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * 인증/인가 설정.
 *
 * <ul>
 *   <li>세션을 쓰지 않는 STATELESS 방식: 서버는 로그인 상태를 저장하지 않고 매 요청의 JWT 만 검증한다.</li>
 *   <li>CSRF 보호 비활성화: 인증 정보를 쿠키가 아닌 Authorization 헤더로 보내므로 CSRF 공격 대상이 아니다.
 *       (refresh token 쿠키는 SameSite=Strict 로 다른 사이트 요청에 포함되지 않는다.)</li>
 *   <li>URL 단위 권한 규칙은 이 클래스에, "본인 티켓만 조회" 같은 데이터 단위 규칙은 서비스 계층에 둔다.</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String ADMIN = "ADMIN";

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtTokenProvider tokenProvider,
                                                   RestAuthenticationEntryPoint authenticationEntryPoint,
                                                   RestAccessDeniedHandler accessDeniedHandler) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults()) // 아래 corsConfigurationSource 빈을 사용
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 누구나
                        .requestMatchers(HttpMethod.POST, "/api/auth/signup", "/api/auth/login",
                                "/api/auth/refresh", "/api/auth/logout").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/codes").permitAll()
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/actuator/health/**", "/error").permitAll()
                        // IT 관리자 전용
                        .requestMatchers("/api/users/**", "/api/dashboard/**").hasRole(ADMIN)
                        .requestMatchers(HttpMethod.POST, "/api/assets/**").hasRole(ADMIN)
                        .requestMatchers(HttpMethod.PATCH, "/api/assets/**").hasRole(ADMIN)
                        .requestMatchers(HttpMethod.DELETE, "/api/assets/**").hasRole(ADMIN)
                        .requestMatchers(HttpMethod.POST, "/api/tickets/*/assign").hasRole(ADMIN)
                        .requestMatchers(HttpMethod.PATCH, "/api/tickets/*/classification").hasRole(ADMIN)
                        // 그 외는 로그인만 하면 가능 (본인 데이터 제한은 서비스에서 처리)
                        .anyRequest().authenticated())
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(authenticationEntryPoint) // 401: 로그인 안 함 / 토큰 만료
                        .accessDeniedHandler(accessDeniedHandler))          // 403: 권한 없음
                .addFilterBefore(new JwtAuthenticationFilter(tokenProvider), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /**
     * {bcrypt} 접두어를 붙여 저장하는 위임형 인코더. 나중에 해시 알고리즘을 바꿔도 기존 비밀번호를 그대로 검증할 수 있다.
     * BCrypt 는 비밀번호마다 무작위 salt 를 섞어 같은 비밀번호라도 매번 다른 해시가 만들어진다.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    /** 프론트엔드가 다른 출처(도메인/포트)에서 호출할 때만 필요. 허용 출처는 설정으로 관리한다. */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(@Value("${app.cors.allowed-origins}") List<String> allowedOrigins) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
