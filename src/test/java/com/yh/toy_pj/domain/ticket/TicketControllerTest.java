package com.yh.toy_pj.domain.ticket;

import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.yh.toy_pj.domain.ticket.dto.TicketResponse;
import com.yh.toy_pj.global.error.BusinessException;
import com.yh.toy_pj.global.error.ErrorCode;
import com.yh.toy_pj.support.Fixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 웹 계층 슬라이스 테스트: 요청 검증, 응답 포맷, 예외 → HTTP 상태 매핑을 검증한다.
 */
@WebMvcTest(TicketController.class)
@ActiveProfiles("test")
class TicketControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TicketService ticketService;

    @Test
    @DisplayName("티켓 접수 성공 시 201 과 Location 헤더를 반환한다")
    void openTicket() throws Exception {
        Ticket ticket = Fixtures.ticket(Fixtures.withId(Fixtures.employee(), 1L), TicketPriority.HIGH);
        org.springframework.test.util.ReflectionTestUtils.setField(ticket, "id", 10L);
        given(ticketService.open(any())).willReturn(TicketResponse.of(ticket, Fixtures.NOW));

        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "VPN 접속 불가", "description": "연결이 안 됩니다", "requesterId": 1}
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
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "", "description": "내용"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("C001"))
                .andExpect(jsonPath("$.errors[*].field", hasItem("title")))
                .andExpect(jsonPath("$.errors[*].field", hasItem("requesterId")));
    }

    @Test
    @DisplayName("정의되지 않은 코드 값이 오면 400 을 반환한다")
    void invalidEnum() throws Exception {
        mockMvc.perform(patch("/api/tickets/1/status")
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
        mockMvc.perform(get("/api/tickets").param("status", "UNKNOWN"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("존재하지 않는 티켓은 404 와 에러 코드를 반환한다")
    void notFound() throws Exception {
        given(ticketService.getDetail(999L)).willThrow(new BusinessException(ErrorCode.TICKET_NOT_FOUND));

        mockMvc.perform(get("/api/tickets/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("T001"));
    }

    @Test
    @DisplayName("허용되지 않은 상태 변경은 409 를 반환한다")
    void invalidTransition() throws Exception {
        given(ticketService.changeStatus(any(), any(), any()))
                .willThrow(new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION, "'접수대기' 상태에서 '종료' 상태로 변경할 수 없습니다."));

        mockMvc.perform(patch("/api/tickets/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "CLOSED"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("T002"))
                .andExpect(jsonPath("$.message").value("'접수대기' 상태에서 '종료' 상태로 변경할 수 없습니다."));
    }
}
