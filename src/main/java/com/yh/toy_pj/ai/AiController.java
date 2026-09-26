package com.yh.toy_pj.ai;

import com.yh.toy_pj.ai.dto.ChatRequest;
import com.yh.toy_pj.ai.dto.ChatResponse;
import com.yh.toy_pj.ai.dto.TriageRequest;
import com.yh.toy_pj.ai.dto.TriageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "AI", description = "Gemini 기반 헬프데스크 보조 기능")
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private static final String CHAT_INSTRUCTION = """
            너는 사내 IT 헬프데스크 도우미다. 임직원의 IT 관련 질문에 한국어로 간결하게 답한다.
            사용자가 직접 해볼 수 있는 조치를 단계별로 안내하고, 해결되지 않으면 헬프데스크 티켓 접수를 권한다.
            """;

    private final GeminiClient geminiClient;
    private final TicketTriageService triageService;

    @Operation(summary = "IT 헬프데스크 챗봇", description = "AI 키가 설정되지 않았거나 장애 시 503(AI001)을 반환한다.")
    @PostMapping("/chat")
    public ChatResponse chat(@Valid @RequestBody ChatRequest request) {
        return new ChatResponse(geminiClient.generate(CHAT_INSTRUCTION, request.message()));
    }

    @Operation(summary = "티켓 분류 미리보기", description = "접수 전 AI 분류 결과를 미리 확인한다. AI 불가 시 키워드 규칙 결과를 반환한다.")
    @PostMapping("/triage")
    public TriageResult triage(@Valid @RequestBody TriageRequest request) {
        return triageService.triage(request.title(), request.description());
    }
}
