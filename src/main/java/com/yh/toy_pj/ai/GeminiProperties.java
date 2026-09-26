package com.yh.toy_pj.ai;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Gemini 연동 설정. API 키는 절대 코드/저장소에 두지 않고 환경변수(GEMINI_API_KEY)로 주입한다.
 */
@ConfigurationProperties(prefix = "gemini")
public record GeminiProperties(
        String apiKey,
        @DefaultValue("https://generativelanguage.googleapis.com") String baseUrl,
        @DefaultValue("gemini-2.5-flash") String model,
        @DefaultValue("3s") Duration connectTimeout,
        @DefaultValue("20s") Duration readTimeout
) {
}
