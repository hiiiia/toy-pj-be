package com.yh.toy_pj.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.yh.toy_pj.domain.ticket.ClassificationSource;
import com.yh.toy_pj.domain.ticket.Ticket;
import com.yh.toy_pj.domain.ticket.TicketCategory;
import com.yh.toy_pj.domain.ticket.TicketPriority;
import com.yh.toy_pj.domain.ticket.TicketRepository;
import com.yh.toy_pj.domain.user.User;
import com.yh.toy_pj.domain.user.UserRole;
import com.yh.toy_pj.support.IntegrationTestSupport;
import jakarta.persistence.EntityManager;
import java.time.Clock;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;

class NotificationIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private SlaMonitor slaMonitor;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private Clock clock;

    @Autowired
    private EntityManager em;

    private User hong;
    private User adminKim;
    private User adminLee;
    private String requester;
    private String admin;

    @BeforeEach
    void setUp() throws Exception {
        hong = createUser("홍길동", "hong@daon.example", UserRole.USER);
        adminKim = createUser("김관리", "admin@daon.example", UserRole.ADMIN);
        adminLee = createUser("이지원", "support@daon.example", UserRole.ADMIN);
        requester = login("hong@daon.example");
        admin = login("admin@daon.example");
    }

    @Test
    @DisplayName("담당자 배정·댓글 알림: 받을 사람에게만 가고, 읽음 처리할 수 있다")
    void assignAndCommentNotifications() throws Exception {
        long ticketId = idOf(postJson("/api/tickets", requester, """
                {"title": "VPN 오류", "description": "접속 불가", "category": "NETWORK", "priority": "HIGH"}
                """));

        // 담당자가 없을 때 요청자 댓글 → 관리자 전체
        postJson("/api/tickets/" + ticketId + "/comments", requester, "{\"content\": \"급합니다\"}");
        assertThat(unread(adminKim)).isEqualTo(1);
        assertThat(unread(adminLee)).isEqualTo(1);

        // 김관리가 이지원에게 배정 → 이지원에게 배정 알림
        postJson("/api/tickets/" + ticketId + "/assign", admin, "{\"assigneeId\": " + adminLee.getId() + "}");
        assertThat(unread(adminLee)).isEqualTo(2);

        // 관리자 일반 댓글 → 요청자 + 담당자(이지원), 작성자(김관리)는 제외
        postJson("/api/tickets/" + ticketId + "/comments", admin, "{\"content\": \"확인 중입니다\"}");
        assertThat(unread(hong)).isEqualTo(1);
        assertThat(unread(adminLee)).isEqualTo(3);
        assertThat(unread(adminKim)).isEqualTo(1);

        // 내부 메모 → 요청자에게는 알리지 않음
        postJson("/api/tickets/" + ticketId + "/comments", admin, "{\"content\": \"장비 교체 필요\", \"internal\": true}");
        assertThat(unread(hong)).isEqualTo(1);

        // 알림 API: 내 알림만, 읽음 처리
        String body = mockMvc.perform(get("/api/notifications").header(HttpHeaders.AUTHORIZATION, requester))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(1))
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].type").value("COMMENT_ADDED"))
                .andExpect(jsonPath("$.items[0].ticketId").value(ticketId))
                .andReturn().getResponse().getContentAsString();
        Number notificationId = JsonPath.read(body, "$.items[0].id");

        // 남의 알림은 읽음 처리할 수 없다
        mockMvc.perform(post("/api/notifications/" + notificationId + "/read").header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/notifications/" + notificationId + "/read").header(HttpHeaders.AUTHORIZATION, requester))
                .andExpect(status().isNoContent());
        assertThat(unread(hong)).isZero();

        mockMvc.perform(post("/api/notifications/read-all").header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isNoContent());
        assertThat(unread(adminKim)).isZero();
    }

    @Test
    @DisplayName("SLA: 임박/초과 티켓에 알림을 보내고, 같은 기한에는 다시 보내지 않으며, 재분류하면 발송 기록이 초기화된다")
    void slaNotifications() {
        LocalDateTime now = LocalDateTime.now(clock); // 서버와 같은 시계(Asia/Seoul) 기준
        // 긴급(4시간): 5시간 전 접수 → 1시간 초과 / 3시간 30분 전 접수 → 30분 남음 / 방금 접수 → 여유
        Ticket overdue = saveTicket("서버 다운", TicketPriority.URGENT, now.minusHours(5));
        Ticket soon = saveTicket("VPN 장애", TicketPriority.URGENT, now.minusMinutes(210));
        saveTicket("프린터", TicketPriority.URGENT, now);
        overdue.assign(adminLee, adminKim);
        ticketRepository.flush();

        SlaMonitor.Result first = slaMonitor.check();

        assertThat(first.breached()).isEqualTo(1);
        assertThat(first.warned()).isEqualTo(1);
        // 초과 티켓은 담당자(이지원)에게만, 담당자 없는 임박 티켓은 관리자 전체에게
        assertThat(countOf(adminLee, NotificationType.SLA_BREACHED)).isEqualTo(1);
        assertThat(countOf(adminKim, NotificationType.SLA_BREACHED)).isZero();
        assertThat(countOf(adminKim, NotificationType.SLA_WARNING)).isEqualTo(1);
        assertThat(countOf(adminLee, NotificationType.SLA_WARNING)).isEqualTo(1);

        // 다시 점검해도 같은 알림은 보내지 않는다
        SlaMonitor.Result second = slaMonitor.check();
        assertThat(second.breached()).isZero();
        assertThat(second.warned()).isZero();

        // 벌크 UPDATE 로 기록된 발송 시각을 확인하려면 1차 캐시를 비우고 DB 에서 다시 읽어야 한다
        em.clear();
        Ticket reloaded = ticketRepository.findById(overdue.getId()).orElseThrow();
        assertThat(reloaded.getSlaBreachedAt()).isNotNull();
        assertThat(reloaded.getSlaWarnedAt()).isNotNull();

        // 재분류로 기한이 바뀌면 발송 기록이 초기화되어, 새 기한 기준으로 다시 알릴 수 있다
        reloaded.reclassify(TicketCategory.HARDWARE, TicketPriority.LOW, now, userRepository.getReferenceById(adminKim.getId()));
        ticketRepository.flush();
        em.clear();
        Ticket afterReclassify = ticketRepository.findById(overdue.getId()).orElseThrow();
        assertThat(afterReclassify.getSlaBreachedAt()).isNull();
        assertThat(afterReclassify.getSlaWarnedAt()).isNull();
    }

    private Ticket saveTicket(String title, TicketPriority priority, LocalDateTime openedAt) {
        return ticketRepository.save(Ticket.open(title, "내용", TicketCategory.ETC, priority,
                ClassificationSource.MANUAL, hong, null, openedAt));
    }

    private long unread(User user) {
        return notificationRepository.countByRecipientIdAndReadAtIsNull(user.getId());
    }

    private long countOf(User user, NotificationType type) {
        return notificationRepository.findAll().stream()
                .filter(n -> n.getRecipient().getId().equals(user.getId()) && n.getType() == type)
                .count();
    }
}
