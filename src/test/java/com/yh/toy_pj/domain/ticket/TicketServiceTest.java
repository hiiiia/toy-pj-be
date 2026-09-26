package com.yh.toy_pj.domain.ticket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.yh.toy_pj.ai.TicketTriageService;
import com.yh.toy_pj.ai.dto.TriageResult;
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

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

    @Mock
    private TicketRepository ticketRepository;
    @Mock
    private UserService userService;
    @Mock
    private AssetService assetService;
    @Mock
    private TicketTriageService triageService;

    private TicketService ticketService;

    @BeforeEach
    void setUp() {
        Clock fixed = Clock.fixed(Fixtures.NOW.atZone(ZONE).toInstant(), ZONE);
        ticketService = new TicketService(ticketRepository, userService, assetService, triageService, fixed);
    }

    private void saveReturnsArgument() {
        given(ticketRepository.save(any(Ticket.class))).willAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @DisplayName("분류/우선순위를 모두 지정하면 자동 분류를 호출하지 않는다")
    void manualClassification() {
        saveReturnsArgument();
        given(userService.getUser(1L)).willReturn(Fixtures.withId(Fixtures.employee(), 1L));
        var request = new TicketCreateRequest("VPN", "접속 불가", TicketCategory.NETWORK, TicketPriority.HIGH, 1L, null);

        TicketResponse response = ticketService.open(request);

        assertThat(response.classificationSource()).isEqualTo(ClassificationSource.MANUAL);
        assertThat(response.dueAt()).isEqualTo(Fixtures.NOW.plusHours(8));
        verify(triageService, never()).triage(anyString(), anyString());
    }

    @Test
    @DisplayName("비어 있는 항목만 자동 분류 결과로 채운다")
    void fillsMissingFieldsFromTriage() {
        saveReturnsArgument();
        given(userService.getUser(1L)).willReturn(Fixtures.withId(Fixtures.employee(), 1L));
        given(triageService.triage(anyString(), anyString())).willReturn(
                new TriageResult(TicketCategory.HARDWARE, TicketPriority.URGENT, ClassificationSource.AI, "근거"));
        var request = new TicketCreateRequest("모니터", "화면 깨짐", TicketCategory.ETC, null, 1L, null);

        TicketResponse response = ticketService.open(request);

        assertThat(response.category()).isEqualTo(TicketCategory.ETC); // 사용자가 지정한 값 유지
        assertThat(response.priority()).isEqualTo(TicketPriority.URGENT); // 자동 분류 값
        assertThat(response.classificationSource()).isEqualTo(ClassificationSource.AI);
    }

    @Test
    @DisplayName("요청자가 존재하지 않으면 티켓을 저장하지 않는다")
    void requesterNotFound() {
        given(userService.getUser(99L)).willThrow(new BusinessException(ErrorCode.USER_NOT_FOUND));
        var request = new TicketCreateRequest("t", "d", TicketCategory.ETC, TicketPriority.LOW, 99L, null);

        assertThatThrownBy(() -> ticketService.open(request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.USER_NOT_FOUND);
        verify(ticketRepository, never()).save(any());
    }
}
