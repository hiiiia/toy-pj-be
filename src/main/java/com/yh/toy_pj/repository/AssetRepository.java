package com.yh.toy_pj.repository;

import com.yh.toy_pj.entity.Asset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AssetRepository extends JpaRepository<Asset, Long> {
    long countByStatus(String status);
}