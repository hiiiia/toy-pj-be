package com.yh.toy_pj.auth.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 모든 요청에서 한 번 실행되는 필터.
 * "Authorization: Bearer {토큰}" 헤더가 유효하면 SecurityContext 에 로그인 사용자를 등록한다.
 *
 * 토큰이 없거나 잘못되어도 여기서 바로 거절하지 않는다.
 * 로그인이 필요한 URL 인지는 SecurityConfig 의 규칙이 판단하고, 필요하면 401 을 응답한다.
 *
 * (@Component 로 등록하면 서블릿 필터로도 자동 등록되어 두 번 실행될 수 있어, SecurityConfig 에서 직접 생성한다.)
 */
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /** 임시 비밀번호 상태의 사용자에게만 주는 권한. 비밀번호 변경·내 정보·로그아웃만 허용된다. */
    public static final String PASSWORD_CHANGE_REQUIRED = "PASSWORD_CHANGE_REQUIRED";

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider tokenProvider;
    private final AccessTokenVerifier tokenVerifier;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            tokenProvider.parse(header.substring(BEARER_PREFIX.length())).ifPresent(token -> {
                String role = switch (tokenVerifier.verify(token)) {
                    case VALID -> token.user().role().name();
                    case PASSWORD_CHANGE_REQUIRED -> PASSWORD_CHANGE_REQUIRED;
                    case REVOKED -> null; // 비밀번호가 바뀌기 전에 발급된 토큰 → 로그인하지 않은 것으로 취급(401)
                };
                if (role != null) {
                    var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));
                    var authentication = new UsernamePasswordAuthenticationToken(token.user(), null, authorities);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            });
        }
        chain.doFilter(request, response);
    }
}
