package com.yh.toy_pj.integration;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.yh.toy_pj.domain.user.UserRole;
import com.yh.toy_pj.support.IntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

/**
 * 핵심 업무 시나리오를 로그인부터 끝까지 검증한다.
 * (AI 키가 없는 test 프로필이므로 자동 분류는 키워드 규칙으로 동작)
 */
class HelpdeskFlowIntegrationTest extends IntegrationTestSupport {

    private long employeeId;
    private String employee;
    private String admin;

    @BeforeEach
    void setUp() throws Exception {
        employeeId = createUser("홍길동", "hong@daon.example", UserRole.USER).getId();
        createUser("김관리", "admin@daon.example", UserRole.ADMIN);
        employee = login("hong@daon.example");
        admin = login("admin@daon.example");
    }

    @Test
    @DisplayName("시나리오: 자산 배정 → 장애 티켓 접수(자동 분류) → 담당자 지정 → 처리 → 해결 → 종료 → 대시보드 반영")
    void fullTicketLifecycle() throws Exception {
        long assetId = idOf(postJson("/api/assets", admin, """
                {"name": "MacBook Pro 14", "type": "LAPTOP", "serialNumber": "SN-001"}
                """).andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("AVAILABLE")));

        postJson("/api/assets/" + assetId + "/assign", admin, "{\"userId\": " + employeeId + "}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_USE"))
                .andExpect(jsonPath("$.assignedUserName").value("홍길동"));

        // 요청자는 본문이 아닌 로그인 정보로 결정되고, category/priority 미지정 → 키워드 규칙으로 자동 분류
        long ticketId = idOf(postJson("/api/tickets", employee, """
                {"title": "노트북 전원이 안 켜집니다", "description": "부팅이 안 되고 업무 불가 상태입니다", "assetId": %d}
                """.formatted(assetId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.requesterName").value("홍길동"))
                .andExpect(jsonPath("$.category").value("HARDWARE"))
                .andExpect(jsonPath("$.priority").value("HIGH")) // 한 사람의 업무 불가 → 높음
                .andExpect(jsonPath("$.classificationSource").value("RULE"))
                .andExpect(jsonPath("$.assetName").value("MacBook Pro 14")));

        // 담당자 없이 처리 시작 불가
        patchJson("/api/tickets/" + ticketId + "/status", admin, "{\"status\": \"IN_PROGRESS\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("T004"));

        // 일반 사용자는 담당자가 될 수 없음
        postJson("/api/tickets/" + ticketId + "/assign", admin, "{\"assigneeId\": " + employeeId + "}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("T003"));

        long adminId = userRepository.findByEmail("admin@daon.example").orElseThrow().getId();
        postJson("/api/tickets/" + ticketId + "/assign", admin, "{\"assigneeId\": " + adminId + "}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticket.assigneeName").value("김관리"));

        patchJson("/api/tickets/" + ticketId + "/status", admin, "{\"status\": \"IN_PROGRESS\", \"note\": \"점검 시작\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nextStatuses", hasSize(2)));
        patchJson("/api/tickets/" + ticketId + "/status", admin, "{\"status\": \"RESOLVED\", \"note\": \"메인보드 교체\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticket.resolvedAt").exists());
        patchJson("/api/tickets/" + ticketId + "/status", admin, "{\"status\": \"CLOSED\"}")
                .andExpect(status().isOk());

        // 요청자도 본인 티켓의 진행 상황과 처리자를 확인할 수 있다
        mockMvc.perform(get("/api/tickets/" + ticketId).header(HttpHeaders.AUTHORIZATION, employee))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticket.status").value("CLOSED"))
                .andExpect(jsonPath("$.nextStatuses", hasSize(0)))
                .andExpect(jsonPath("$.histories", hasSize(5)))
                .andExpect(jsonPath("$.histories[0].actorName").value("홍길동"))
                .andExpect(jsonPath("$.histories[4].actorName").value("김관리"))
                .andExpect(jsonPath("$.histories[4].toStatus").value("CLOSED"));

        mockMvc.perform(get("/api/dashboard/summary").header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tickets.total").value(1))
                .andExpect(jsonPath("$.tickets.byStatus.CLOSED").value(1))
                .andExpect(jsonPath("$.assets.byStatus.IN_USE").value(1));

        // 티켓 이력이 있는 자산은 삭제 불가 (사용중이므로 먼저 반납)
        postJson("/api/assets/" + assetId + "/return", admin, "").andExpect(status().isOk());
        mockMvc.perform(delete("/api/assets/" + assetId).header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("A004"));
    }

    @Test
    @DisplayName("일반 사용자는 본인 티켓만 보이고, 남의 티켓·관리자 기능에는 접근할 수 없다")
    void employeeDataScope() throws Exception {
        createUser("김철수", "kim@daon.example", UserRole.USER);
        String other = login("kim@daon.example");

        long myTicket = idOf(postJson("/api/tickets", employee, """
                {"title": "VPN 문의", "description": "설정 방법", "category": "NETWORK", "priority": "LOW"}
                """).andExpect(status().isCreated()));
        long othersTicket = idOf(postJson("/api/tickets", other, """
                {"title": "모니터 요청", "description": "추가", "category": "HARDWARE", "priority": "LOW"}
                """).andExpect(status().isCreated()));

        // requesterId 조건을 조작해도 본인 티켓만 조회된다
        mockMvc.perform(get("/api/tickets").param("requesterId", "999").header(HttpHeaders.AUTHORIZATION, employee))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(myTicket));

        mockMvc.perform(get("/api/tickets/" + othersTicket).header(HttpHeaders.AUTHORIZATION, employee))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH004"));

        // 관리자 전용 API
        mockMvc.perform(get("/api/dashboard/summary").header(HttpHeaders.AUTHORIZATION, employee))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/users").header(HttpHeaders.AUTHORIZATION, employee))
                .andExpect(status().isForbidden());
        postJson("/api/assets", employee, "{\"name\": \"A\", \"type\": \"LAPTOP\", \"serialNumber\": \"X\"}")
                .andExpect(status().isForbidden());

        // 본인 티켓 처리는 불가, 취소만 가능
        patchJson("/api/tickets/" + myTicket + "/status", employee, "{\"status\": \"IN_PROGRESS\"}")
                .andExpect(status().isForbidden());
        patchJson("/api/tickets/" + myTicket + "/status", employee, "{\"status\": \"CANCELED\", \"note\": \"자체 해결\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticket.status").value("CANCELED"));

        // 관리자는 전체 티켓을 본다
        mockMvc.perform(get("/api/tickets").header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(jsonPath("$.totalElements").value(2));

        // 대시보드 카드에서 넘어오는 조건: 미완료(active) = 취소된 티켓 제외
        mockMvc.perform(get("/api/tickets").param("active", "true").header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(jsonPath("$.totalElements").value(1));
        // 방금 접수한 티켓이라 SLA 초과는 없음
        mockMvc.perform(get("/api/tickets").param("overdue", "true").header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @DisplayName("검색: 상태/키워드 조건과 페이징 응답 포맷")
    void searchTickets() throws Exception {
        for (int i = 1; i <= 3; i++) {
            postJson("/api/tickets", employee, """
                    {"title": "VPN 문의 %d", "description": "설정 방법", "category": "NETWORK", "priority": "LOW"}
                    """.formatted(i)).andExpect(status().isCreated());
        }
        postJson("/api/tickets", employee, """
                {"title": "모니터 요청", "description": "추가", "category": "HARDWARE", "priority": "LOW"}
                """).andExpect(status().isCreated());

        mockMvc.perform(get("/api/tickets").param("keyword", "vpn").param("size", "2").header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.hasNext").value(true))
                .andExpect(jsonPath("$.content[0].title").value("VPN 문의 3")); // 기본 정렬: 최신순
    }

    @Test
    @DisplayName("중복 이메일/시리얼 번호는 409 를 반환한다")
    void duplicates() throws Exception {
        postJson("/api/users", admin, """
                {"name": "홍길동2", "email": "hong@daon.example", "password": "password1", "role": "USER"}
                """).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("U002"));

        postJson("/api/assets", admin, """
                {"name": "A", "type": "LAPTOP", "serialNumber": "SN-DUP"}
                """).andExpect(status().isCreated());
        postJson("/api/assets", admin, """
                {"name": "B", "type": "LAPTOP", "serialNumber": "SN-DUP"}
                """).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("A002"));
    }

    @Test
    @DisplayName("AI 키가 없으면 챗봇은 503, 분류 미리보기는 키워드 규칙 결과를 반환한다")
    void aiGracefulDegradation() throws Exception {
        postJson("/api/ai/chat", employee, "{\"message\": \"VPN 설정 방법 알려줘\"}")
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("AI001"));

        postJson("/api/ai/triage", employee, "{\"title\": \"비밀번호 초기화\", \"description\": \"계정이 잠겼어요\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.category").value("ACCOUNT"))
                .andExpect(jsonPath("$.source").value("RULE"));
    }

    @Test
    @DisplayName("공통 코드는 로그인 없이 조회되고, 존재하지 않는 경로는 404")
    void codesAndNotFound() throws Exception {
        mockMvc.perform(get("/api/codes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticketStatus[0].code").value("OPEN"))
                .andExpect(jsonPath("$.ticketStatus[0].label").value("접수대기"));

        // 에러 응답에는 서버 로그를 찾을 수 있는 요청 ID 가 담기고, 응답 헤더 값과 같다
        mockMvc.perform(get("/api/unknown").header(HttpHeaders.AUTHORIZATION, admin).header("X-Request-Id", "trace-404"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("C002"))
                .andExpect(jsonPath("$.requestId").value("trace-404"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header().string("X-Request-Id", "trace-404"));
    }
}
