package com.yh.toy_pj.global.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 요청마다 추적 ID(requestId)를 붙인다.
 *
 * <ul>
 *   <li>앞단(nginx)이 보낸 X-Request-Id 가 있으면 그대로 쓰고, 없으면 새로 만든다.</li>
 *   <li>MDC 에 넣어 이 요청에서 찍히는 모든 로그 앞에 [requestId] 가 붙는다. (logging.pattern.correlation)</li>
 *   <li>응답 헤더와 에러 응답 본문에도 넣어, 사용자가 "요청 ID: xxx" 를 알려주면 해당 로그를 바로 찾을 수 있다.</li>
 *   <li>요청 1건마다 "메서드 URI → 상태 (소요시간)" 접근 로그를 남긴다.</li>
 * </ul>
 * 보안 필터보다 먼저 실행되어야 401/403 응답에도 ID 가 붙으므로 가장 높은 우선순위로 등록한다.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Request-Id";
    public static final String MDC_KEY = "requestId";

    /** 외부에서 들어온 값은 로그 위조(개행 삽입 등)를 막기 위해 형식을 검사한다. */
    private static final Pattern VALID_ID = Pattern.compile("[A-Za-z0-9._-]{1,64}");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String requestId = resolveRequestId(request.getHeader(HEADER));
        long start = System.nanoTime();
        MDC.put(MDC_KEY, requestId);
        response.setHeader(HEADER, requestId);
        try {
            chain.doFilter(request, response);
        } finally {
            if (request.getRequestURI().startsWith("/api/")) {
                long ms = (System.nanoTime() - start) / 1_000_000;
                log.info("{} {} → {} ({}ms)", request.getMethod(), request.getRequestURI(), response.getStatus(), ms);
            }
            MDC.remove(MDC_KEY);
        }
    }

    static String resolveRequestId(String incoming) {
        if (incoming != null && VALID_ID.matcher(incoming).matches()) {
            return incoming;
        }
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    /** 현재 요청의 ID (요청 밖이면 null) */
    public static String current() {
        return MDC.get(MDC_KEY);
    }
}
