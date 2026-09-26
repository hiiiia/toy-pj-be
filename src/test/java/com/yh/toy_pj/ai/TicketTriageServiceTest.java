package com.yh.toy_pj.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.yh.toy_pj.ai.dto.TriageResult;
import com.yh.toy_pj.domain.ticket.ClassificationSource;
import com.yh.toy_pj.domain.ticket.TicketCategory;
import com.yh.toy_pj.domain.ticket.TicketPriority;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class TicketTriageServiceTest {

    @Mock
    private GeminiClient geminiClient;

    private TicketTriageService triageService;

    @BeforeEach
    void setUp() {
        triageService = new TicketTriageService(geminiClient, new RuleBasedTriage(), JsonMapper.builder().build());
    }

    @Test
    @DisplayName("AI 키가 없으면 AI 를 호출하지 않고 키워드 규칙으로 분류한다")
    void fallsBackWhenAiUnavailable() {
        given(geminiClient.isAvailable()).willReturn(false);

        TriageResult result = triageService.triage("VPN 안됨", "접속 불가");

        assertThat(result.source()).isEqualTo(ClassificationSource.RULE);
        verify(geminiClient, never()).generateJson(anyString(), anyString());
    }

    @Test
    @DisplayName("AI 가 올바른 JSON 을 주면 AI 분류 결과를 사용한다")
    void usesAiResult() {
        given(geminiClient.isAvailable()).willReturn(true);
        given(geminiClient.generateJson(anyString(), anyString()))
                .willReturn("{\"category\":\"network\",\"priority\":\"URGENT\",\"reason\":\"전사 장애\"}");

        TriageResult result = triageService.triage("인터넷 장애", "전 층 인터넷이 끊겼습니다");

        assertThat(result.source()).isEqualTo(ClassificationSource.AI);
        assertThat(result.category()).isEqualTo(TicketCategory.NETWORK);
        assertThat(result.priority()).isEqualTo(TicketPriority.URGENT);
        assertThat(result.reason()).isEqualTo("전사 장애");
    }

    @Test
    @DisplayName("AI 호출이 실패하면 키워드 규칙으로 대체한다")
    void fallsBackWhenAiFails() {
        given(geminiClient.isAvailable()).willReturn(true);
        given(geminiClient.generateJson(anyString(), anyString())).willThrow(new AiException("timeout"));

        TriageResult result = triageService.triage("모니터 고장", "화면이 안 나옵니다");

        assertThat(result.source()).isEqualTo(ClassificationSource.RULE);
        assertThat(result.category()).isEqualTo(TicketCategory.HARDWARE);
    }

    @Test
    @DisplayName("AI 가 정의되지 않은 값이나 깨진 JSON 을 주면 키워드 규칙으로 대체한다")
    void fallsBackWhenAiResponseInvalid() {
        given(geminiClient.isAvailable()).willReturn(true);
        given(geminiClient.generateJson(anyString(), anyString()))
                .willReturn("{\"category\":\"PRINTER\",\"priority\":\"HIGH\"}")
                .willReturn("not a json");

        assertThat(triageService.triage("프린터", "용지 걸림").source()).isEqualTo(ClassificationSource.RULE);
        assertThat(triageService.triage("프린터", "용지 걸림").source()).isEqualTo(ClassificationSource.RULE);
    }
}
