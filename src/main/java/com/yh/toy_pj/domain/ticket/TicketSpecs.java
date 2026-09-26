package com.yh.toy_pj.domain.ticket;

import com.yh.toy_pj.domain.ticket.dto.TicketSearchCondition;
import java.time.LocalDateTime;
import com.yh.toy_pj.global.common.LikePatterns;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

/**
 * 티켓 검색 조건을 동적으로 조합한다. 값이 없는 조건은 무시된다.
 */
final class TicketSpecs {

    private TicketSpecs() {
    }

    /** @param now SLA 초과 여부를 판단할 기준 시각 (서비스의 Clock 에서 받아 테스트에서 고정할 수 있게 한다) */
    static Specification<Ticket> of(TicketSearchCondition cond, LocalDateTime now) {
        List<Specification<Ticket>> specs = new ArrayList<>();
        // "미완료"와 "SLA 초과"는 대시보드 숫자와 같은 기준(TicketStatus.ACTIVE)을 써야 클릭했을 때 건수가 일치한다
        if (Boolean.TRUE.equals(cond.active()) || Boolean.TRUE.equals(cond.overdue())) {
            specs.add((root, query, cb) -> root.get("status").in(TicketStatus.ACTIVE));
        }
        if (Boolean.TRUE.equals(cond.overdue())) {
            specs.add((root, query, cb) -> cb.lessThan(root.get("dueAt"), now));
        }
        if (cond.status() != null) {
            specs.add((root, query, cb) -> cb.equal(root.get("status"), cond.status()));
        }
        if (cond.priority() != null) {
            specs.add((root, query, cb) -> cb.equal(root.get("priority"), cond.priority()));
        }
        if (cond.category() != null) {
            specs.add((root, query, cb) -> cb.equal(root.get("category"), cond.category()));
        }
        if (cond.requesterId() != null) {
            specs.add((root, query, cb) -> cb.equal(root.get("requester").get("id"), cond.requesterId()));
        }
        if (cond.assigneeId() != null) {
            specs.add((root, query, cb) -> cb.equal(root.get("assignee").get("id"), cond.assigneeId()));
        }
        if (Boolean.TRUE.equals(cond.unassigned())) {
            specs.add((root, query, cb) -> cb.isNull(root.get("assignee")));
        }
        if (StringUtils.hasText(cond.keyword())) {
            String pattern = LikePatterns.containsIgnoreCase(cond.keyword());
            specs.add((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("title")), pattern, LikePatterns.ESCAPE),
                    cb.like(cb.lower(root.get("description")), pattern, LikePatterns.ESCAPE)));
        }
        return Specification.allOf(specs);
    }
}
