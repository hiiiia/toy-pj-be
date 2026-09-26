package com.yh.toy_pj.domain.ticket;

import com.yh.toy_pj.domain.ticket.dto.TicketSearchCondition;
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

    static Specification<Ticket> of(TicketSearchCondition cond) {
        List<Specification<Ticket>> specs = new ArrayList<>();
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
            String pattern = "%" + cond.keyword().trim().toLowerCase() + "%";
            specs.add((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("title")), pattern),
                    cb.like(cb.lower(root.get("description")), pattern)));
        }
        return Specification.allOf(specs);
    }
}
