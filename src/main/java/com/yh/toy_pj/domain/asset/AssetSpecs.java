package com.yh.toy_pj.domain.asset;

import com.yh.toy_pj.domain.asset.dto.AssetSearchCondition;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

/**
 * 자산 검색 조건을 동적으로 조합한다. 값이 없는 조건은 무시된다.
 */
final class AssetSpecs {

    private AssetSpecs() {
    }

    static Specification<Asset> of(AssetSearchCondition cond) {
        List<Specification<Asset>> specs = new ArrayList<>();
        if (cond.status() != null) {
            specs.add((root, query, cb) -> cb.equal(root.get("status"), cond.status()));
        }
        if (cond.type() != null) {
            specs.add((root, query, cb) -> cb.equal(root.get("type"), cond.type()));
        }
        if (cond.assignedUserId() != null) {
            specs.add((root, query, cb) -> cb.equal(root.get("assignedUser").get("id"), cond.assignedUserId()));
        }
        if (StringUtils.hasText(cond.keyword())) {
            String pattern = "%" + cond.keyword().trim().toLowerCase() + "%";
            specs.add((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("name")), pattern),
                    cb.like(cb.lower(root.get("serialNumber")), pattern)));
        }
        return Specification.allOf(specs);
    }
}
