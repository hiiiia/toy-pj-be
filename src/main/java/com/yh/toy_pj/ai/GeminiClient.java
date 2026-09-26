package com.yh.toy_pj.ai;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Google Gemini generateContent API 클라이언트.
 *
 * <ul>
 *   <li>요청 본문은 문자열 연결이 아닌 객체 직렬화로 생성 → 사용자 입력에 따옴표/개행이 있어도 JSON 이 깨지지 않음</li>
 *   <li>API 키는 URL 쿼리가 아닌 헤더(x-goog-api-key)로 전달 → 접근 로그에 키가 남지 않음</li>
 *   <li>연결/응답 타임아웃 설정 → 외부 API 지연이 서버 스레드를 붙잡지 않도록 함</li>
 * </ul>
 */
@Slf4j
@Component
public class GeminiClient {

    private final GeminiProperties properties;
    private final RestClient restClient;

    public GeminiClient(GeminiProperties properties) {
        this.properties = properties;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.connectTimeout());
        requestFactory.setReadTimeout(properties.readTimeout());
        this.restClient = RestClient.builder()
                .baseUrl(properties.baseUrl())
                .requestFactory(requestFactory)
                .build();
    }

    public boolean isAvailable() {
        return StringUtils.hasText(properties.apiKey());
    }

    /** 일반 텍스트 응답 */
    public String generate(String systemInstruction, String prompt) {
        return call(systemInstruction, prompt, null);
    }

    /** JSON 형식 응답 (Gemini structured output) */
    public String generateJson(String systemInstruction, String prompt) {
        return call(systemInstruction, prompt, "application/json");
    }

    private String call(String systemInstruction, String prompt, String responseMimeType) {
        if (!isAvailable()) {
            throw new AiException("Gemini API 키가 설정되지 않았습니다. 환경변수 GEMINI_API_KEY 를 확인하세요.");
        }
        GenerateRequest request = new GenerateRequest(
                systemInstruction != null ? Content.of(systemInstruction) : null,
                List.of(Content.of(prompt)),
                new GenerationConfig(responseMimeType, 0.2));
        try {
            GenerateResponse response = restClient.post()
                    .uri("/v1beta/models/{model}:generateContent", properties.model())
                    .header("x-goog-api-key", properties.apiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(GenerateResponse.class);
            return extractText(response)
                    .orElseThrow(() -> new AiException("Gemini 응답에서 텍스트를 찾을 수 없습니다."));
        } catch (RestClientException e) {
            log.warn("Gemini API 호출 실패: {}", e.getMessage());
            throw new AiException("Gemini API 호출에 실패했습니다.", e);
        }
    }

    private Optional<String> extractText(GenerateResponse response) {
        return Optional.ofNullable(response)
                .map(GenerateResponse::candidates)
                .filter(candidates -> !candidates.isEmpty())
                .map(candidates -> candidates.get(0).content())
                .map(Content::parts)
                .filter(parts -> !parts.isEmpty())
                .map(parts -> parts.get(0).text())
                .filter(StringUtils::hasText);
    }

    // ===== Gemini API 요청/응답 스펙 =====

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record GenerateRequest(Content systemInstruction, List<Content> contents, GenerationConfig generationConfig) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record GenerationConfig(String responseMimeType, Double temperature) {
    }

    record Content(List<Part> parts) {
        static Content of(String text) {
            return new Content(List.of(new Part(text)));
        }
    }

    record Part(String text) {
    }

    record GenerateResponse(List<Candidate> candidates) {
    }

    record Candidate(Content content) {
    }
}
