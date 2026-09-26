package com.yh.toy_pj.integration;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

/**
 * 실제 스프링 컨텍스트 + H2 로 핵심 업무 시나리오를 처음부터 끝까지 검증한다.
 * (AI 키가 없는 test 프로필이므로 자동 분류는 키워드 규칙으로 동작)
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class HelpdeskFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("시나리오: 자산 배정 → 장애 티켓 접수(자동 분류) → 담당자 지정 → 처리 → 해결 → 종료 → 대시보드 반영")
    void fullTicketLifecycle() throws Exception {
        long employeeId = createUser("홍길동", "hong@daon.example", "USER");
        long adminId = createUser("김관리", "admin@daon.example", "ADMIN");

        long assetId = idOf(postJson("/api/assets", """
                {"name": "MacBook Pro 14", "type": "LAPTOP", "serialNumber": "SN-001"}
                """).andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("AVAILABLE")));

        postJson("/api/assets/" + assetId + "/assign", "{\"userId\": " + employeeId + "}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_USE"))
                .andExpect(jsonPath("$.assignedUserName").value("홍길동"));

        // category/priority 미지정 → 키워드 규칙으로 자동 분류
        long ticketId = idOf(postJson("/api/tickets", """
                {"title": "노트북 전원이 안 켜집니다", "description": "부팅이 안 되고 업무 불가 상태입니다",
                 "requesterId": %d, "assetId": %d}
                """.formatted(employeeId, assetId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.category").value("HARDWARE"))
                .andExpect(jsonPath("$.priority").value("URGENT"))
                .andExpect(jsonPath("$.classificationSource").value("RULE"))
                .andExpect(jsonPath("$.assetName").value("MacBook Pro 14")));

        // 담당자 없이 처리 시작 불가
        patchJson("/api/tickets/" + ticketId + "/status", "{\"status\": \"IN_PROGRESS\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("T004"));

        // 일반 사용자는 담당자가 될 수 없음
        postJson("/api/tickets/" + ticketId + "/assign", "{\"assigneeId\": " + employeeId + "}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("T003"));

        postJson("/api/tickets/" + ticketId + "/assign", "{\"assigneeId\": " + adminId + "}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticket.assigneeName").value("김관리"));

        patchJson("/api/tickets/" + ticketId + "/status", "{\"status\": \"IN_PROGRESS\", \"note\": \"점검 시작\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nextStatuses", hasSize(2)));
        patchJson("/api/tickets/" + ticketId + "/status", "{\"status\": \"RESOLVED\", \"note\": \"메인보드 교체\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticket.resolvedAt").exists());
        patchJson("/api/tickets/" + ticketId + "/status", "{\"status\": \"CLOSED\"}")
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/tickets/" + ticketId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticket.status").value("CLOSED"))
                .andExpect(jsonPath("$.nextStatuses", hasSize(0)))
                .andExpect(jsonPath("$.histories", hasSize(5)))
                .andExpect(jsonPath("$.histories[4].fromStatus").value("RESOLVED"))
                .andExpect(jsonPath("$.histories[4].toStatus").value("CLOSED"));

        mockMvc.perform(get("/api/dashboard/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tickets.total").value(1))
                .andExpect(jsonPath("$.tickets.byStatus.CLOSED").value(1))
                .andExpect(jsonPath("$.tickets.byStatus.OPEN").value(0))
                .andExpect(jsonPath("$.assets.byStatus.IN_USE").value(1));

        // 티켓 이력이 있는 자산은 삭제 불가 (사용중이므로 먼저 반납)
        postJson("/api/assets/" + assetId + "/return", "").andExpect(status().isOk());
        mockMvc.perform(delete("/api/assets/" + assetId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("A004"));
    }

    @Test
    @DisplayName("검색: 상태/키워드 조건과 페이징 응답 포맷")
    void searchTickets() throws Exception {
        long employeeId = createUser("홍길동", "hong@daon.example", "USER");
        for (int i = 1; i <= 3; i++) {
            postJson("/api/tickets", """
                    {"title": "VPN 문의 %d", "description": "설정 방법", "category": "NETWORK", "priority": "LOW", "requesterId": %d}
                    """.formatted(i, employeeId)).andExpect(status().isCreated());
        }
        postJson("/api/tickets", """
                {"title": "모니터 요청", "description": "추가", "category": "HARDWARE", "priority": "LOW", "requesterId": %d}
                """.formatted(employeeId)).andExpect(status().isCreated());

        mockMvc.perform(get("/api/tickets").param("keyword", "vpn").param("size", "2"))
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
        createUser("홍길동", "hong@daon.example", "USER");
        postJson("/api/users", """
                {"name": "홍길동2", "email": "hong@daon.example", "role": "USER"}
                """).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("U002"));

        postJson("/api/assets", """
                {"name": "A", "type": "LAPTOP", "serialNumber": "SN-DUP"}
                """).andExpect(status().isCreated());
        postJson("/api/assets", """
                {"name": "B", "type": "LAPTOP", "serialNumber": "SN-DUP"}
                """).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("A002"));
    }

    @Test
    @DisplayName("AI 키가 없으면 챗봇은 503, 분류 미리보기는 키워드 규칙 결과를 반환한다")
    void aiGracefulDegradation() throws Exception {
        postJson("/api/ai/chat", "{\"message\": \"VPN 설정 방법 알려줘\"}")
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("AI001"));

        postJson("/api/ai/triage", "{\"title\": \"비밀번호 초기화\", \"description\": \"계정이 잠겼어요\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.category").value("ACCOUNT"))
                .andExpect(jsonPath("$.source").value("RULE"));
    }

    @Test
    @DisplayName("공통 코드와 존재하지 않는 경로 처리")
    void codesAndNotFound() throws Exception {
        mockMvc.perform(get("/api/codes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticketStatus[0].code").value("OPEN"))
                .andExpect(jsonPath("$.ticketStatus[0].label").value("접수대기"));

        mockMvc.perform(get("/api/unknown"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("C002"));
    }

    private long createUser(String name, String email, String role) throws Exception {
        return idOf(postJson("/api/users", """
                {"name": "%s", "email": "%s", "department": "테스트팀", "role": "%s"}
                """.formatted(name, email, role)).andExpect(status().isCreated()));
    }

    private ResultActions postJson(String url, String body) throws Exception {
        return mockMvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions patchJson(String url, String body) throws Exception {
        return mockMvc.perform(patch(url).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private long idOf(ResultActions actions) throws Exception {
        Number id = JsonPath.read(actions.andReturn().getResponse().getContentAsString(), "$.id");
        return id.longValue();
    }
}
