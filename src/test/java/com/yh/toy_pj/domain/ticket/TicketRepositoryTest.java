package com.yh.toy_pj.domain.ticket;

import static org.assertj.core.api.Assertions.assertThat;

import com.yh.toy_pj.domain.ticket.dto.TicketSearchCondition;
import com.yh.toy_pj.domain.user.User;
import com.yh.toy_pj.global.config.JpaConfig;
import com.yh.toy_pj.support.Fixtures;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@Import(JpaConfig.class)
@ActiveProfiles("test")
class TicketRepositoryTest {

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private TestEntityManager em;

    private User requester;
    private User admin;

    @BeforeEach
    void setUp() {
        requester = em.persist(Fixtures.employee());
        admin = em.persist(Fixtures.admin());

        Ticket vpn = Ticket.open("VPN 접속 불가", "재택 VPN 오류", TicketCategory.NETWORK, TicketPriority.URGENT,
                ClassificationSource.MANUAL, requester, null, Fixtures.NOW);
        Ticket monitor = Ticket.open("모니터 깜빡임", "화면이 깜빡입니다", TicketCategory.HARDWARE, TicketPriority.LOW,
                ClassificationSource.RULE, requester, null, Fixtures.NOW);
        monitor.assign(admin);
        monitor.changeStatus(TicketStatus.IN_PROGRESS, null, Fixtures.NOW);
        Ticket canceled = Ticket.open("VPN 재문의", "중복", TicketCategory.NETWORK, TicketPriority.URGENT,
                ClassificationSource.MANUAL, requester, null, Fixtures.NOW);
        canceled.changeStatus(TicketStatus.CANCELED, "중복 접수", Fixtures.NOW);

        ticketRepository.saveAll(List.of(vpn, monitor, canceled));
        em.flush();
        em.clear();
    }

    @Test
    @DisplayName("동적 검색: 조건이 없으면 전체, 조건이 있으면 조합해서 필터링한다")
    void searchWithSpecification() {
        Page<Ticket> all = ticketRepository.findAll(TicketSpecs.of(condition(null, null, null, null)), page());
        Page<Ticket> vpnOpen = ticketRepository.findAll(TicketSpecs.of(condition(TicketStatus.OPEN, null, null, "vpn")), page());
        Page<Ticket> byAssignee = ticketRepository.findAll(TicketSpecs.of(condition(null, admin.getId(), null, null)), page());
        Page<Ticket> unassigned = ticketRepository.findAll(TicketSpecs.of(condition(null, null, true, null)), page());

        assertThat(all.getTotalElements()).isEqualTo(3);
        assertThat(vpnOpen.getContent()).extracting(Ticket::getTitle).containsExactly("VPN 접속 불가");
        assertThat(byAssignee.getContent()).extracting(Ticket::getTitle).containsExactly("모니터 깜빡임");
        assertThat(unassigned.getTotalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("상세 조회 시 처리 이력을 함께 가져온다")
    void findDetailWithHistories() {
        Long id = ticketRepository.findAll(TicketSpecs.of(condition(TicketStatus.IN_PROGRESS, null, null, null)), page())
                .getContent().get(0).getId();
        em.clear();

        Ticket ticket = ticketRepository.findDetailById(id).orElseThrow();

        assertThat(ticket.getHistories()).extracting(TicketHistory::getToStatus)
                .containsExactly(TicketStatus.OPEN, TicketStatus.OPEN, TicketStatus.IN_PROGRESS);
        assertThat(ticket.getHistories().get(0).getCreatedAt()).isNotNull(); // JPA Auditing
    }

    @Test
    @DisplayName("상태별/우선순위별 건수를 GROUP BY 로 집계한다")
    void aggregate() {
        assertThat(ticketRepository.countGroupByStatus())
                .extracting(TicketRepository.KeyCount::getCode, TicketRepository.KeyCount::getTotal)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple(TicketStatus.OPEN, 1L),
                        org.assertj.core.groups.Tuple.tuple(TicketStatus.IN_PROGRESS, 1L),
                        org.assertj.core.groups.Tuple.tuple(TicketStatus.CANCELED, 1L));

        assertThat(ticketRepository.countGroupByPriority(TicketStatus.ACTIVE))
                .extracting(TicketRepository.KeyCount::getCode, TicketRepository.KeyCount::getTotal)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple(TicketPriority.URGENT, 1L),
                        org.assertj.core.groups.Tuple.tuple(TicketPriority.LOW, 1L));
    }

    @Test
    @DisplayName("SLA 초과 건수는 진행중 상태만 대상으로 한다")
    void countOverdue() {
        LocalDateTime fiveHoursLater = Fixtures.NOW.plusHours(5);   // URGENT(4h) 초과, LOW(72h) 미초과
        LocalDateTime fourDaysLater = Fixtures.NOW.plusDays(4);     // 둘 다 초과 (취소 건 제외)

        assertThat(ticketRepository.countOverdue(TicketStatus.ACTIVE, fiveHoursLater)).isEqualTo(1);
        assertThat(ticketRepository.countOverdue(TicketStatus.ACTIVE, fourDaysLater)).isEqualTo(2);
        assertThat(ticketRepository.countByAssigneeIsNullAndStatus(TicketStatus.OPEN)).isEqualTo(1);
    }

    private TicketSearchCondition condition(TicketStatus status, Long assigneeId, Boolean unassigned, String keyword) {
        return new TicketSearchCondition(status, null, null, null, assigneeId, unassigned, keyword);
    }

    private PageRequest page() {
        return PageRequest.of(0, 10, Sort.by("id"));
    }
}
