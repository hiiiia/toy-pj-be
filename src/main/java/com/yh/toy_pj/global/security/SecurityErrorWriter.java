package com.yh.toy_pj.global.security;

import com.yh.toy_pj.global.error.ErrorCode;
import com.yh.toy_pj.global.error.ErrorResponse;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * 시큐리티 필터 단계의 에러는 컨트롤러에 도달하기 전에 발생해 GlobalExceptionHandler 가 처리하지 못한다.
 * 그래서 같은 ErrorResponse 포맷으로 직접 응답을 작성한다.
 */
@Component
@RequiredArgsConstructor
public class SecurityErrorWriter {

    private final JsonMapper jsonMapper;

    public void write(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        response.setStatus(errorCode.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(jsonMapper.writeValueAsString(ErrorResponse.of(errorCode, errorCode.getMessage())));
    }
}
