package com.yh.toy_pj.global.common;

import java.util.List;
import org.springframework.data.domain.Page;

/**
 * Page 구현체(PageImpl)를 그대로 직렬화하면 응답 구조가 Spring 내부 구현에 종속되므로 전용 응답 DTO로 감싼다.
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext
) {

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext()
        );
    }
}
