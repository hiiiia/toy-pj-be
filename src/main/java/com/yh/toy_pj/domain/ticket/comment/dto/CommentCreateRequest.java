package com.yh.toy_pj.domain.ticket.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * @param internal IT 관리자 전용 내부 메모 여부. 생략하면 일반 댓글.
 *                 (primitive boolean 이면 Jackson 3 에서 필드 생략 시 역직렬화 오류가 나므로 Boolean 사용)
 */
public record CommentCreateRequest(@NotBlank @Size(max = 2000) String content, Boolean internal) {

    public boolean isInternal() {
        return Boolean.TRUE.equals(internal);
    }
}
