package com.yh.toy_pj.global.logging;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.FilterChain;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RequestIdFilterTest {

    private final RequestIdFilter filter = new RequestIdFilter();

    @Test
    @DisplayName("요청 처리 중에는 MDC 에 requestId 가 있고, 응답 헤더로도 내려가며, 끝나면 MDC 에서 지워진다")
    void putsRequestIdInMdcAndHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/tickets");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> duringRequest = new AtomicReference<>();
        FilterChain chain = (req, res) -> duringRequest.set(MDC.get(RequestIdFilter.MDC_KEY));

        filter.doFilter(request, response, chain);

        assertThat(duringRequest.get()).hasSize(16);
        assertThat(response.getHeader(RequestIdFilter.HEADER)).isEqualTo(duringRequest.get());
        assertThat(MDC.get(RequestIdFilter.MDC_KEY)).isNull(); // 스레드 재사용 시 다른 요청에 섞이지 않도록
    }

    @Test
    @DisplayName("앞단(nginx)이 보낸 X-Request-Id 는 그대로 이어서 쓴다")
    void reusesIncomingId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/tickets");
        request.addHeader(RequestIdFilter.HEADER, "nginx-abc123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> {
        });

        assertThat(response.getHeader(RequestIdFilter.HEADER)).isEqualTo("nginx-abc123");
    }

    @Test
    @DisplayName("형식이 잘못된 값(개행 삽입 등 로그 위조 시도)은 버리고 새로 만든다")
    void rejectsMaliciousId() {
        assertThat(RequestIdFilter.resolveRequestId("abc\n2026-01-01 INFO 가짜 로그")).hasSize(16).doesNotContain("\n");
        assertThat(RequestIdFilter.resolveRequestId("x".repeat(100))).hasSize(16);
    }
}
