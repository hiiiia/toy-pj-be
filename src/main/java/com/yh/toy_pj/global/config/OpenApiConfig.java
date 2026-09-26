package com.yh.toy_pj.global.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI().info(new Info()
                .title("IT 헬프데스크 & 자산관리 API")
                .description("사내 IT 자산 관리, 장애/요청 티켓 처리, AI 기반 티켓 자동 분류 API")
                .version("v0.1.0"));
    }
}
