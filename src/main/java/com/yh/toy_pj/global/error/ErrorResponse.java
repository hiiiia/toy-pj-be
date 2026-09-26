package com.yh.toy_pj.global.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 모든 API 에러 응답의 공통 포맷.
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ErrorResponse(
        String code,
        String message,
        int status,
        List<FieldError> errors,
        LocalDateTime timestamp
) {

    public static ErrorResponse of(ErrorCode errorCode, String message) {
        return new ErrorResponse(errorCode.getCode(), message, errorCode.getStatus().value(), List.of(), LocalDateTime.now());
    }

    public static ErrorResponse of(ErrorCode errorCode, List<FieldError> errors) {
        return new ErrorResponse(errorCode.getCode(), errorCode.getMessage(), errorCode.getStatus().value(), errors, LocalDateTime.now());
    }

    public record FieldError(String field, Object rejectedValue, String reason) {
    }
}
