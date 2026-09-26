package com.yh.toy_pj.domain.user;

import java.time.LocalDateTime;

/**
 * 매 요청마다 access token 을 검증할 때 필요한 계정 상태만 담은 조회 전용 값.
 * (User 엔티티 전체를 읽지 않고 필요한 두 컬럼만 조회한다)
 */
public record AccountStatus(LocalDateTime passwordChangedAt, boolean mustChangePassword) {
}
