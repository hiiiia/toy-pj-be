package com.yh.toy_pj.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class GeminiService {

    @Value("${gemini.api.key}")
    private String apiKey;

    private final RestClient restClient = RestClient.create();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public String callGemini(String prompt) {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + apiKey;
        String requestBody = "{ \"contents\": [{ \"parts\": [{ \"text\": \"" + prompt + "\" }] }] }";

        String jsonResponse = restClient.post()
                .uri(url)
                .header("Content-Type", "application/json")
                .body(requestBody)
                .retrieve()
                .body(String.class);

        try {
            // JSON 응답 구조(candidates -> content -> parts -> text)에서 순수 텍스트만 추출
            JsonNode rootNode = objectMapper.readTree(jsonResponse);
            JsonNode textNode = rootNode.path("candidates").path(0)
                    .path("content").path("parts").path(0)
                    .path("text");

            if (!textNode.isMissingNode()) {
                return textNode.asText();
            } else {
                return "AI 응답 구조를 파싱할 수 없습니다.";
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "JSON 파싱 중 오류가 발생했습니다: " + e.getMessage();
        }
    }
}