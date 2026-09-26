package com.yh.toy_pj.domain.ticket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.yh.toy_pj.domain.user.UserRole;
import com.yh.toy_pj.support.IntegrationTestSupport;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.ResultActions;

/** 티켓 댓글 · 첨부파일 */
class TicketCollaborationIntegrationTest extends IntegrationTestSupport {

    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 1, 2, 3};

    private String requester;
    private String other;
    private String admin;
    private long ticketId;

    @BeforeEach
    void setUp() throws Exception {
        createUser("홍길동", "hong@daon.example", UserRole.USER);
        createUser("김철수", "kim@daon.example", UserRole.USER);
        createUser("김관리", "admin@daon.example", UserRole.ADMIN);
        requester = login("hong@daon.example");
        other = login("kim@daon.example");
        admin = login("admin@daon.example");
        ticketId = idOf(postJson("/api/tickets", requester, """
                {"title": "모니터 깜빡임", "description": "화면이 계속 깜빡입니다", "category": "HARDWARE", "priority": "MEDIUM"}
                """).andExpect(status().isCreated()));
    }

    @Test
    @DisplayName("댓글: 요청자·관리자가 대화하고, 내부 메모는 관리자에게만 보인다")
    void comments() throws Exception {
        postJson(commentsUrl(), requester, "{\"content\": \"오전부터 증상이 있었습니다\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.authorName").value("홍길동"));
        postJson(commentsUrl(), admin, "{\"content\": \"케이블 교체 예정\", \"internal\": true}")
                .andExpect(status().isCreated());
        postJson(commentsUrl(), admin, "{\"content\": \"오후에 방문드리겠습니다\"}")
                .andExpect(status().isCreated());

        // 요청자에게는 내부 메모가 보이지 않는다
        mockMvc.perform(get(commentsUrl()).header(HttpHeaders.AUTHORIZATION, requester))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[1].content").value("오후에 방문드리겠습니다"));
        mockMvc.perform(get(commentsUrl()).header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[1].internal").value(true));

        // 일반 사용자는 내부 메모를 쓸 수 없고, 남의 티켓에는 댓글을 볼 수도 쓸 수도 없다
        postJson(commentsUrl(), requester, "{\"content\": \"몰래\", \"internal\": true}").andExpect(status().isForbidden());
        mockMvc.perform(get(commentsUrl()).header(HttpHeaders.AUTHORIZATION, other)).andExpect(status().isForbidden());
        postJson(commentsUrl(), other, "{\"content\": \"끼어들기\"}").andExpect(status().isForbidden());
        postJson(commentsUrl(), requester, "{\"content\": \"  \"}").andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("댓글 삭제는 작성자만, 종료된 티켓에는 댓글을 쓸 수 없다")
    void commentRules() throws Exception {
        long commentId = idOf(postJson(commentsUrl(), requester, "{\"content\": \"추가 정보\"}"));

        mockMvc.perform(delete(commentsUrl() + "/" + commentId).header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete(commentsUrl() + "/" + commentId).header(HttpHeaders.AUTHORIZATION, requester))
                .andExpect(status().isNoContent());

        patchJson("/api/tickets/" + ticketId + "/status", requester, "{\"status\": \"CANCELED\"}").andExpect(status().isOk());
        postJson(commentsUrl(), requester, "{\"content\": \"취소 후 댓글\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("T005"));
    }

    @Test
    @DisplayName("첨부: 업로드 → 목록 → 다운로드(원래 파일명, 서버가 정한 형식, nosniff) → 삭제")
    void attachmentLifecycle() throws Exception {
        long attachmentId = idOf(upload(requester, new MockMultipartFile("file", "화면 캡처.png", "image/png", PNG))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.filename").value("화면 캡처.png"))
                .andExpect(jsonPath("$.contentType").value("image/png"))
                .andExpect(jsonPath("$.size").value(PNG.length)));

        mockMvc.perform(get(attachmentsUrl()).header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].uploaderName").value("홍길동"));

        byte[] downloaded = mockMvc.perform(get(attachmentsUrl() + "/" + attachmentId).header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        org.hamcrest.Matchers.containsString("filename*=UTF-8''" + java.net.URLEncoder
                                .encode("화면 캡처.png", StandardCharsets.UTF_8).replace("+", "%20"))))
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(downloaded).isEqualTo(PNG);

        // 다른 사용자는 다운로드 불가, 관리자는 삭제 가능
        mockMvc.perform(get(attachmentsUrl() + "/" + attachmentId).header(HttpHeaders.AUTHORIZATION, other))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete(attachmentsUrl() + "/" + attachmentId).header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isNoContent());
        mockMvc.perform(get(attachmentsUrl()).header(HttpHeaders.AUTHORIZATION, requester))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("첨부 검증: 허용되지 않은 형식 415, 내용 위조 415, 크기 초과 413, 파일 누락 400")
    void attachmentValidation() throws Exception {
        upload(requester, new MockMultipartFile("file", "page.html", "text/html", "<script>".getBytes()))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value("F003"));
        upload(requester, new MockMultipartFile("file", "fake.png", "image/png", "%PDF-1.4".getBytes()))
                .andExpect(status().isUnsupportedMediaType());
        upload(requester, new MockMultipartFile("file", "big.png", "image/png", bigPng()))
                .andExpect(status().isContentTooLarge())
                .andExpect(jsonPath("$.code").value("F002"));
        mockMvc.perform(multipart(attachmentsUrl()).header(HttpHeaders.AUTHORIZATION, requester))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("티켓당 첨부는 10개까지")
    void attachmentLimit() throws Exception {
        for (int i = 0; i < 10; i++) {
            upload(requester, new MockMultipartFile("file", "s" + i + ".png", "image/png", PNG)).andExpect(status().isCreated());
        }
        upload(requester, new MockMultipartFile("file", "s10.png", "image/png", PNG))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("F004"));
    }

    private static byte[] bigPng() {
        byte[] data = new byte[5 * 1024 * 1024 + 1];
        System.arraycopy(PNG, 0, data, 0, PNG.length);
        return data;
    }

    private ResultActions upload(String token, MockMultipartFile file) throws Exception {
        return mockMvc.perform(multipart(attachmentsUrl()).file(file).header(HttpHeaders.AUTHORIZATION, token));
    }

    private String commentsUrl() {
        return "/api/tickets/" + ticketId + "/comments";
    }

    private String attachmentsUrl() {
        return "/api/tickets/" + ticketId + "/attachments";
    }
}
