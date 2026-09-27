package com.yh.toy_pj.global.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 서비스 전역 에러 코드. 프론트엔드는 message 대신 code 로 분기할 수 있다.
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Common
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "C001", "입력값이 올바르지 않습니다."),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "C002", "요청한 리소스를 찾을 수 없습니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "C003", "지원하지 않는 HTTP 메서드입니다."),
    DATA_INTEGRITY_VIOLATION(HttpStatus.CONFLICT, "C004", "데이터 무결성 제약 조건에 위배됩니다."),
    CONCURRENT_MODIFICATION(HttpStatus.CONFLICT, "C005", "다른 사용자가 먼저 수정했습니다. 새로고침 후 다시 시도하세요."),
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "C006", "지원하지 않는 Content-Type 입니다."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C999", "서버 내부 오류가 발생했습니다."),

    // Auth
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "AUTH001", "로그인이 필요합니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "AUTH002", "이메일 또는 비밀번호가 올바르지 않습니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH003", "로그인이 만료되었습니다. 다시 로그인하세요."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "AUTH004", "접근 권한이 없습니다."),
    ACCOUNT_LOCKED(HttpStatus.LOCKED, "AUTH005", "로그인에 여러 번 실패해 계정이 잠겼습니다."),
    CURRENT_PASSWORD_MISMATCH(HttpStatus.BAD_REQUEST, "AUTH006", "현재 비밀번호가 올바르지 않습니다."),
    SAME_AS_CURRENT_PASSWORD(HttpStatus.BAD_REQUEST, "AUTH007", "새 비밀번호가 현재 비밀번호와 같습니다."),
    PASSWORD_CHANGE_REQUIRED(HttpStatus.FORBIDDEN, "AUTH008", "임시 비밀번호로 로그인했습니다. 비밀번호를 먼저 변경하세요."),
    SOCIAL_EMAIL_REQUIRED(HttpStatus.BAD_REQUEST, "AUTH009", "SNS 계정의 이메일 제공 동의가 필요합니다."),
    SOCIAL_EMAIL_ALREADY_REGISTERED(HttpStatus.CONFLICT, "AUTH010", "이미 이메일로 가입된 계정입니다. 이메일 로그인을 이용해 주세요."),
    SOCIAL_LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "AUTH011", "SNS 로그인에 실패했습니다. 다시 시도해 주세요."),

    // User
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "U001", "사용자를 찾을 수 없습니다."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "U002", "이미 등록된 이메일입니다."),

    // Asset
    ASSET_NOT_FOUND(HttpStatus.NOT_FOUND, "A001", "자산을 찾을 수 없습니다."),
    DUPLICATE_SERIAL_NUMBER(HttpStatus.CONFLICT, "A002", "이미 등록된 시리얼 번호입니다."),
    INVALID_ASSET_STATE(HttpStatus.CONFLICT, "A003", "현재 자산 상태에서 수행할 수 없는 작업입니다."),
    ASSET_HAS_TICKETS(HttpStatus.CONFLICT, "A004", "티켓 이력이 있는 자산은 삭제할 수 없습니다. 폐기 처리를 이용하세요."),

    // Ticket
    TICKET_NOT_FOUND(HttpStatus.NOT_FOUND, "T001", "티켓을 찾을 수 없습니다."),
    INVALID_STATUS_TRANSITION(HttpStatus.CONFLICT, "T002", "허용되지 않는 티켓 상태 변경입니다."),
    ASSIGNEE_NOT_ADMIN(HttpStatus.BAD_REQUEST, "T003", "IT 관리자(ADMIN)만 티켓 담당자로 지정할 수 있습니다."),
    ASSIGNEE_REQUIRED(HttpStatus.CONFLICT, "T004", "담당자가 지정되어야 처리를 시작할 수 있습니다."),
    TICKET_ALREADY_FINISHED(HttpStatus.CONFLICT, "T005", "이미 종료되었거나 취소된 티켓입니다."),
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "T006", "댓글을 찾을 수 없습니다."),

    // Attachment
    ATTACHMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "F001", "첨부파일을 찾을 수 없습니다."),
    FILE_TOO_LARGE(HttpStatus.CONTENT_TOO_LARGE, "F002", "파일 크기가 너무 큽니다."),
    UNSUPPORTED_FILE_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "F003", "허용되지 않는 파일 형식입니다."),
    ATTACHMENT_LIMIT_EXCEEDED(HttpStatus.CONFLICT, "F004", "티켓당 첨부파일 개수를 초과했습니다."),
    FILE_STORAGE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "F005", "파일을 저장하거나 읽는 중 오류가 발생했습니다."),

    // AI
    AI_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "AI001", "AI 서비스를 사용할 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
