package com.yh.toy_pj.domain.asset;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface AssetRepository extends JpaRepository<Asset, Long>, JpaSpecificationExecutor<Asset> {

    boolean existsBySerialNumber(String serialNumber);

    /** 목록 조회 시 배정 사용자를 함께 조회해 N+1 을 방지한다. */
    @Override
    @EntityGraph(attributePaths = "assignedUser")
    Page<Asset> findAll(Specification<Asset> spec, Pageable pageable);

    @Query("select a.status as status, count(a) as total from Asset a group by a.status")
    List<StatusCount> countGroupByStatus();

    interface StatusCount {
        AssetStatus getStatus();

        long getTotal();
    }
}
