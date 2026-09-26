package com.yh.toy_pj.global.common;

import com.yh.toy_pj.global.error.BusinessException;
import com.yh.toy_pj.global.error.ErrorCode;
import java.util.Set;
import java.util.TreeSet;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * 목록 API 의 sort 파라미터를 허용된 필드로만 제한한다.
 *
 * 제한하지 않으면
 *  - 없는 필드(?sort=foo)는 Spring Data 의 PropertyReferenceException → 500 이 되고,
 *  - 연관 엔티티의 민감 필드(?sort=requester.password)로 정렬해 응답 순서로 정보를 추측할 수 있다.
 */
public final class SortPolicy {

    private SortPolicy() {
    }

    public static Pageable restrict(Pageable pageable, Set<String> allowedProperties) {
        for (Sort.Order order : pageable.getSort()) {
            if (!allowedProperties.contains(order.getProperty())) {
                throw new BusinessException(ErrorCode.INVALID_INPUT,
                        "정렬할 수 없는 항목입니다: " + order.getProperty() + " (가능: " + String.join(", ", new TreeSet<>(allowedProperties)) + ")");
            }
        }
        return pageable;
    }
}
