package com.yh.toy_pj.domain.ticket;

import static com.yh.toy_pj.support.Fixtures.NOW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.yh.toy_pj.domain.user.User;
import com.yh.toy_pj.global.error.BusinessException;
import com.yh.toy_pj.global.error.ErrorCode;
import com.yh.toy_pj.support.Fixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

class TicketTest {

    private static final User ADMIN = Fixtures.admin();

    @Nested
    @DisplayName("티켓 접수")
    class Open {

        @Test
        @DisplayName("접수대기 상태로 생성되고 접수 이력이 남는다")
        void opensWithHistory() {
            Ticket ticket = Fixtures.ticket(Fixtures.employee(), TicketPriority.MEDIUM);

            assertThat(ticket.getStatus()).isEqualTo(TicketStatus.OPEN);
            assertThat(ticket.getHistories()).hasSize(1);
            assertThat(ticket.getHistories().get(0).getToStatus()).isEqualTo(TicketStatus.OPEN);
        }

        @ParameterizedTest(name = "{0} 우선순위 → {1}시간 후 처리 기한")
        @CsvSource({"LOW, 72", "MEDIUM, 24", "HIGH, 8", "URGENT, 4"})
        @DisplayName("우선순위별 SLA 로 처리 기한이 계산된다")
        void calculatesDueAtBySla(TicketPriority priority, long hours) {
            Ticket ticket = Fixtures.ticket(Fixtures.employee(), priority);

            assertThat(ticket.getDueAt()).isEqualTo(NOW.plusHours(hours));
        }
    }

    @Nested
    @DisplayName("담당자 지정")
    class Assign {

        @Test
        @DisplayName("IT 관리자가 아니면 담당자로 지정할 수 없다")
        void rejectsNonAdmin() {
            Ticket ticket = Fixtures.ticket(Fixtures.employee(), TicketPriority.MEDIUM);

            assertThatThrownBy(() -> ticket.assign(Fixtures.employee(), ADMIN))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.ASSIGNEE_NOT_ADMIN);
        }

        @Test
        @DisplayName("종료된 티켓에는 담당자를 지정할 수 없다")
        void rejectsFinishedTicket() {
            Ticket ticket = Fixtures.ticket(Fixtures.employee(), TicketPriority.MEDIUM);
            ticket.changeStatus(TicketStatus.CANCELED, "중복 접수", NOW, ADMIN);

            assertThatThrownBy(() -> ticket.assign(ADMIN, ADMIN))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.TICKET_ALREADY_FINISHED);
        }
    }

    @Nested
    @DisplayName("상태 변경")
    class ChangeStatus {

        @Test
        @DisplayName("담당자 없이 처리를 시작할 수 없다")
        void requiresAssigneeToStart() {
            Ticket ticket = Fixtures.ticket(Fixtures.employee(), TicketPriority.MEDIUM);

            assertThatThrownBy(() -> ticket.changeStatus(TicketStatus.IN_PROGRESS, null, NOW, ADMIN))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.ASSIGNEE_REQUIRED);
        }

        @Test
        @DisplayName("정상 흐름: 접수 → 처리중 → 해결 → 종료, 모든 변경이 이력으로 남는다")
        void happyPath() {
            Ticket ticket = Fixtures.ticket(Fixtures.employee(), TicketPriority.HIGH);
            ticket.assign(ADMIN, ADMIN);

            ticket.changeStatus(TicketStatus.IN_PROGRESS, "원격 점검 시작", NOW, ADMIN);
            ticket.changeStatus(TicketStatus.RESOLVED, "VPN 인증서 재발급", NOW.plusHours(1), ADMIN);
            ticket.changeStatus(TicketStatus.CLOSED, null, NOW.plusHours(2), ADMIN);

            assertThat(ticket.getStatus()).isEqualTo(TicketStatus.CLOSED);
            assertThat(ticket.getResolvedAt()).isEqualTo(NOW.plusHours(1));
            assertThat(ticket.getHistories()).extracting(TicketHistory::getToStatus)
                    .containsExactly(TicketStatus.OPEN, TicketStatus.OPEN, TicketStatus.IN_PROGRESS,
                            TicketStatus.RESOLVED, TicketStatus.CLOSED);
        }

        @Test
        @DisplayName("해결된 티켓을 재오픈하면 해결 시각이 초기화된다")
        void reopenClearsResolvedAt() {
            Ticket ticket = Fixtures.ticket(Fixtures.employee(), TicketPriority.HIGH);
            ticket.assign(ADMIN, ADMIN);
            ticket.changeStatus(TicketStatus.IN_PROGRESS, null, NOW, ADMIN);
            ticket.changeStatus(TicketStatus.RESOLVED, null, NOW, ADMIN);

            ticket.changeStatus(TicketStatus.IN_PROGRESS, "재발생", NOW.plusHours(3), ADMIN);

            assertThat(ticket.getResolvedAt()).isNull();
        }

        @Test
        @DisplayName("허용되지 않은 전이는 예외가 발생한다 (접수대기 → 종료)")
        void rejectsInvalidTransition() {
            Ticket ticket = Fixtures.ticket(Fixtures.employee(), TicketPriority.MEDIUM);

            assertThatThrownBy(() -> ticket.changeStatus(TicketStatus.CLOSED, null, NOW, ADMIN))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("접수대기")
                    .extracting("errorCode").isEqualTo(ErrorCode.INVALID_STATUS_TRANSITION);
        }
    }

    @Nested
    @DisplayName("상태 변경 권한")
    class Permission {

        @Test
        @DisplayName("요청자는 본인 티켓을 취소할 수 있고, 처리자로 이력에 남는다")
        void requesterCanCancel() {
            User requester = Fixtures.employee();
            Ticket ticket = Fixtures.ticket(requester, TicketPriority.MEDIUM);

            ticket.changeStatus(TicketStatus.CANCELED, "해결됨", NOW, requester);

            assertThat(ticket.getStatus()).isEqualTo(TicketStatus.CANCELED);
            assertThat(ticket.getHistories().get(1).getActor()).isSameAs(requester);
        }

        @Test
        @DisplayName("일반 사용자는 취소 외의 상태 변경을 할 수 없다")
        void requesterCannotProcess() {
            User requester = Fixtures.employee();
            Ticket ticket = Fixtures.ticket(requester, TicketPriority.MEDIUM);
            ticket.assign(ADMIN, ADMIN);

            assertThatThrownBy(() -> ticket.changeStatus(TicketStatus.IN_PROGRESS, null, NOW, requester))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.FORBIDDEN);
        }

        @Test
        @DisplayName("다른 사람의 티켓은 취소할 수 없다")
        void otherUserCannotCancel() {
            Ticket ticket = Fixtures.ticket(Fixtures.withId(Fixtures.employee(), 1L), TicketPriority.MEDIUM);
            User other = Fixtures.withId(Fixtures.employee("김철수", "kim@test.com"), 2L);

            assertThatThrownBy(() -> ticket.changeStatus(TicketStatus.CANCELED, null, NOW, other))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.FORBIDDEN);
        }
    }

    @Nested
    @DisplayName("SLA 초과 여부")
    class Overdue {

        @Test
        @DisplayName("처리 기한이 지난 진행중 티켓은 SLA 초과다")
        void overdueWhenActiveAndPastDue() {
            Ticket ticket = Fixtures.ticket(Fixtures.employee(), TicketPriority.URGENT);

            assertThat(ticket.isOverdue(NOW.plusHours(3))).isFalse();
            assertThat(ticket.isOverdue(NOW.plusHours(5))).isTrue();
        }

        @Test
        @DisplayName("취소된 티켓은 기한이 지나도 SLA 초과로 보지 않는다")
        void finishedTicketIsNotOverdue() {
            Ticket ticket = Fixtures.ticket(Fixtures.employee(), TicketPriority.URGENT);
            ticket.changeStatus(TicketStatus.CANCELED, null, NOW, ADMIN);

            assertThat(ticket.isOverdue(NOW.plusDays(10))).isFalse();
        }
    }

    @Test
    @DisplayName("재분류하면 분류 출처가 MANUAL 로 바뀌고 처리 기한이 재계산된다")
    void reclassify() {
        Ticket ticket = Fixtures.ticket(Fixtures.employee(), TicketPriority.LOW);

        ticket.reclassify(TicketCategory.ACCOUNT, TicketPriority.URGENT, NOW, ADMIN);

        assertThat(ticket.getClassificationSource()).isEqualTo(ClassificationSource.MANUAL);
        assertThat(ticket.getPriority()).isEqualTo(TicketPriority.URGENT);
        assertThat(ticket.getDueAt()).isEqualTo(NOW.plusHours(4));
    }

    @ParameterizedTest
    @EnumSource(value = TicketStatus.class, names = {"CLOSED", "CANCELED"})
    @DisplayName("종료/취소 상태에서는 어떤 상태로도 전이할 수 없다")
    void terminalStatusHasNoNext(TicketStatus terminal) {
        assertThat(terminal.nextStatuses()).isEmpty();
        assertThat(terminal.isFinished()).isTrue();
    }
}
