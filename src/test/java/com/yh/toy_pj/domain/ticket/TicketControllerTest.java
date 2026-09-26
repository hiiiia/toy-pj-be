package com.yh.toy_pj.domain.ticket;

import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.yh.toy_pj.auth.jwt.JwtTokenProvider;
import com.yh.toy_pj.domain.ticket.dto.TicketResponse;
import com.yh.toy_pj.domain.user.User;
import com.yh.toy_pj.global.common.PageResponse;
import com.yh.toy_pj.global.error.BusinessException;
import com.yh.toy_pj.global.error.ErrorCode;
import com.yh.toy_pj.support.Fixtures;
import com.yh.toy_pj.support.WebMvcSecurityTestConfig;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 웹 계층 슬라이스 테스트: 인증/인가, 요청 검증, 응답 포맷, 예외 → HTTP 상태 매핑을 검증한다.
 */
@WebMvcTest(TicketController.class)
@Import(WebMvcSecurityTestConfig.class)
@ActiveProfiles("test")
class TicketControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @MockitoBean
    private TicketService ticketService;

    private String employeeToken;
    private String adminToken;

    @BeforeEach
    void setUp() {
        employeeToken = bearer(Fixtures.withId(Fixtures.employee(), 1L));
        adminToken = bearer(Fixtures.withId(Fixtures.admin(), 2L));
    }

    @Test
    @DisplayName("토큰 없이 호출하면 401 과 AUTH001 을 반환한다")
    void unauthenticated() throws Exception {
        mockMvc.perform(get("/api/tickets"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH001"));
        verifyNoInteractions(ticketService);
    }

    @Test
    @DisplayName("위조된 토큰은 인증되지 않는다")
    void forgedToken() throws Exception {
        mockMvc.perform(get("/api/tickets").header(HttpHeaders.AUTHORIZATION, employeeToken + "tampered"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("일반 사용자가 관리자 API(담당자 지정)를 호출하면 403 과 AUTH004 를 반환한다")
    void forbiddenForEmployee() throws Exception {
        mockMvc.perform(post("/api/tickets/1/assign")
                        .header(HttpHeaders.AUTHORIZATION, employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assigneeId\": 2}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH004"));
        verifyNoInteractions(ticketService);
    }

    @Test
    @DisplayName("티켓 접수 성공 시 201 과 Location 헤더를 반환한다")
    void openTicket() throws Exception {
        Ticket ticket = Fixtures.ticket(Fixtures.withId(Fixtures.employee(), 1L), TicketPriority.HIGH);
        ReflectionTestUtils.setField(ticket, "id", 10L);
        given(ticketService.open(any(), any())).willReturn(TicketResponse.of(ticket, Fixtures.NOW));

        mockMvc.perform(post("/api/tickets")
                        .header(HttpHeaders.AUTHORIZATION, employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "VPN 접속 불가", "description": "연결이 안 됩니다"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/tickets/10"))
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.requesterName").value("홍길동"));
    }

    @Test
    @DisplayName("필수값이 누락되면 400 과 필드별 에러를 반환한다")
    void validationError() throws Exception {
        mockMvc.perform(post("/api/tickets")
                        .header(HttpHeaders.AUTHORIZATION, employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "", "description": ""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("C001"))
                .andExpect(jsonPath("$.errors[*].field", hasItem("title")))
                .andExpect(jsonPath("$.errors[*].field", hasItem("description")));
    }

    @Test
    @DisplayName("정의되지 않은 코드 값이 오면 400 을 반환한다")
    void invalidEnum() throws Exception {
        mockMvc.perform(patch("/api/tickets/1/status")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "DONE"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("C001"));
    }

    @Test
    @DisplayName("검색 조건의 코드 값이 잘못되면 400 을 반환한다")
    void invalidSearchParam() throws Exception {
        mockMvc.perform(get("/api/tickets").param("status", "UNKNOWN").header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("허용되지 않은 필드로 정렬하면 400 (없는 필드로 500 이 나거나 requester.password 같은 민감 필드로 정렬되지 않게)")
    void invalidSortProperty() throws Exception {
        mockMvc.perform(get("/api/tickets").param("sort", "foo").header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("C001"));
        mockMvc.perform(get("/api/tickets").param("sort", "requester.password,asc").header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(ticketService);
    }

    @Test
    @DisplayName("허용된 필드로는 정렬할 수 있다")
    void allowedSortProperty() throws Exception {
        given(ticketService.search(any(), any(), any())).willReturn(new PageResponse<>(List.of(), 0, 20, 0, 0, false));

        mockMvc.perform(get("/api/tickets").param("sort", "dueAt,asc").header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("JSON API 에 다른 Content-Type 으로 보내면 500 이 아니라 415 를 반환한다")
    void unsupportedMediaType() throws Exception {
        mockMvc.perform(post("/api/tickets")
                        .header(HttpHeaders.AUTHORIZATION, employeeToken)
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("title=VPN"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value("C006"));
    }

    @Test
    @DisplayName("존재하지 않는 티켓은 404 와 에러 코드를 반환한다")
    void notFound() throws Exception {
        given(ticketService.getDetail(any(), any())).willThrow(new BusinessException(ErrorCode.TICKET_NOT_FOUND));

        mockMvc.perform(get("/api/tickets/999").header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("T001"));
    }

    @Test
    @DisplayName("허용되지 않은 상태 변경은 409 를 반환한다")
    void invalidTransition() throws Exception {
        given(ticketService.changeStatus(any(), any(), any(), any()))
                .willThrow(new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION, "'접수대기' 상태에서 '종료' 상태로 변경할 수 없습니다."));

        mockMvc.perform(patch("/api/tickets/1/status")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "CLOSED"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("T002"))
                .andExpect(jsonPath("$.message").value("'접수대기' 상태에서 '종료' 상태로 변경할 수 없습니다."));
    }

    private String bearer(User user) {
        return "Bearer " + tokenProvider.createAccessToken(user);
    }
}
