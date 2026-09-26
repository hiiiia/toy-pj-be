package com.yh.toy_pj.domain.ticket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.yh.toy_pj.ai.TicketTriageService;
import com.yh.toy_pj.ai.dto.TriageResult;
import com.yh.toy_pj.auth.AuthUser;
import com.yh.toy_pj.domain.asset.Asset;
import com.yh.toy_pj.domain.user.UserRole;
import java.util.Optional;
import com.yh.toy_pj.domain.asset.AssetService;
import com.yh.toy_pj.domain.ticket.dto.TicketCreateRequest;
import com.yh.toy_pj.domain.ticket.dto.TicketResponse;
import com.yh.toy_pj.domain.user.UserService;
import com.yh.toy_pj.global.error.BusinessException;
import com.yh.toy_pj.global.error.ErrorCode;
import com.yh.toy_pj.support.Fixtures;
import java.time.Clock;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

    private static final AuthUser EMPLOYEE = new AuthUser(1L, "홍길동", UserRole.USER);
    private static final AuthUser ADMIN = new AuthUser(2L, "김관리", UserRole.ADMIN);

    @Mock
    private TicketRepository ticketRepository;
    @Mock
    private UserService userService;
    @Mock
    private AssetService assetService;
    @Mock
    private TicketTriageService triageService;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private TicketService ticketService;

    @BeforeEach
    void setUp() {
        Clock fixed = Clock.fixed(Fixtures.NOW.atZone(ZONE).toInstant(), ZONE);
        // 단위 테스트에서는 실제 트랜잭션 없이 콜백만 실행되도록 가짜 트랜잭션 매니저를 넣는다
        TransactionTemplate tx = new TransactionTemplate(mock(PlatformTransactionManager.class));
        ticketService = new TicketService(ticketRepository, userService, assetService, triageService, eventPublisher, tx, fixed);
    }

    private void saveReturnsArgument() {
        given(ticketRepository.save(any(Ticket.class))).willAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @DisplayName("관리자가 분류/우선순위를 모두 지정하면 자동 분류를 호출하지 않는다")
    void manualClassification() {
        saveReturnsArgument();
        given(userService.getUser(2L)).willReturn(Fixtures.withId(Fixtures.admin(), 2L));
        var request = new TicketCreateRequest("VPN", "접속 불가", TicketCategory.NETWORK, TicketPriority.HIGH, null);

        TicketResponse response = ticketService.open(request, ADMIN);

        assertThat(response.classificationSource()).isEqualTo(ClassificationSource.MANUAL);
        assertThat(response.requesterId()).isEqualTo(2L); // 요청자는 로그인 사용자
        assertThat(response.dueAt()).isEqualTo(Fixtures.NOW.plusHours(8));
        verify(triageService, never()).triage(anyString(), anyString());
    }

    @Test
    @DisplayName("일반 사용자가 보낸 우선순위는 무시하고 자동 분류 결과를 쓴다 (스스로 '긴급'으로 올리지 못하게)")
    void employeeCannotSetPriority() {
        saveReturnsArgument();
        given(userService.getUser(1L)).willReturn(Fixtures.withId(Fixtures.employee(), 1L));
        given(triageService.triage(anyString(), anyString())).willReturn(
                new TriageResult(TicketCategory.NETWORK, TicketPriority.LOW, ClassificationSource.RULE, "근거"));
        var request = new TicketCreateRequest("VPN 문의", "설정 방법", TicketCategory.NETWORK, TicketPriority.URGENT, null);

        TicketResponse response = ticketService.open(request, EMPLOYEE);

        assertThat(response.priority()).isEqualTo(TicketPriority.LOW);
        assertThat(response.category()).isEqualTo(TicketCategory.NETWORK); // 분류는 사용자가 고른 값 유지
    }

    @Test
    @DisplayName("비어 있는 항목만 자동 분류 결과로 채운다")
    void fillsMissingFieldsFromTriage() {
        saveReturnsArgument();
        given(userService.getUser(1L)).willReturn(Fixtures.withId(Fixtures.employee(), 1L));
        given(triageService.triage(anyString(), anyString())).willReturn(
                new TriageResult(TicketCategory.HARDWARE, TicketPriority.URGENT, ClassificationSource.AI, "근거"));
        var request = new TicketCreateRequest("모니터", "화면 깨짐", TicketCategory.ETC, null, null);

        TicketResponse response = ticketService.open(request, EMPLOYEE);

        assertThat(response.category()).isEqualTo(TicketCategory.ETC); // 사용자가 지정한 값 유지
        assertThat(response.priority()).isEqualTo(TicketPriority.URGENT); // 자동 분류 값
        assertThat(response.classificationSource()).isEqualTo(ClassificationSource.AI);
    }

    @Test
    @DisplayName("요청자가 존재하지 않으면 티켓을 저장하지 않는다")
    void requesterNotFound() {
        given(userService.getUser(99L)).willThrow(new BusinessException(ErrorCode.USER_NOT_FOUND));
        var request = new TicketCreateRequest("t", "d", TicketCategory.ETC, TicketPriority.LOW, null);

        assertThatThrownBy(() -> ticketService.open(request, new AuthUser(99L, "탈퇴자", UserRole.USER)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.USER_NOT_FOUND);
        verify(ticketRepository, never()).save(any());
    }

    @Test
    @DisplayName("일반 사용자는 다른 사람에게 배정된 자산을 티켓에 연결할 수 없다")
    void cannotAttachOthersAsset() {
        given(userService.getUser(1L)).willReturn(Fixtures.withId(Fixtures.employee(), 1L));
        Asset othersLaptop = Fixtures.laptop("SN-OTHER");
        othersLaptop.assignTo(Fixtures.withId(Fixtures.employee("김철수", "kim@test.com"), 2L));
        given(assetService.getAsset(5L)).willReturn(othersLaptop);
        var request = new TicketCreateRequest("노트북 고장", "부팅 불가", TicketCategory.HARDWARE, TicketPriority.HIGH, 5L);

        assertThatThrownBy(() -> ticketService.open(request, EMPLOYEE))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.FORBIDDEN);
        verify(ticketRepository, never()).save(any());
    }

    @Test
    @DisplayName("일반 사용자는 다른 사람이 요청한 티켓 상세를 볼 수 없다")
    void cannotViewOthersTicket() {
        Ticket othersTicket = Fixtures.ticket(Fixtures.withId(Fixtures.employee("김철수", "kim@test.com"), 2L), TicketPriority.LOW);
        given(ticketRepository.findDetailById(10L)).willReturn(Optional.of(othersTicket));

        assertThatThrownBy(() -> ticketService.getDetail(10L, EMPLOYEE))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.FORBIDDEN);
    }
}
