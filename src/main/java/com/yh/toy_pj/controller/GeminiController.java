package com.yh.toy_pj.controller;

import com.yh.toy_pj.service.GeminiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
public class GeminiController {

    @Autowired
    private GeminiService geminiService;

    @GetMapping("/api/gemini/test")
    public String testGemini(@RequestParam(defaultValue = "안녕? 다온 플레이스 IT 관리 솔루션이야. 첫 인사를 해줘!") String prompt) {
        return geminiService.callGemini(prompt);
    }

    // 프론트엔드에서 질문을 받아 Gemini와 통신하는 POST API
    @PostMapping("/api/gemini/chat")
    public String chatGemini(@RequestBody java.util.Map<String, String> request) {
        String prompt = request.get("prompt");
        return geminiService.callGemini(prompt);
    }
}