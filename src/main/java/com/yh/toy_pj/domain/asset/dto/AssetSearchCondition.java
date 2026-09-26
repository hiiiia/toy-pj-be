package com.yh.toy_pj.domain.asset.dto;

import com.yh.toy_pj.domain.asset.AssetStatus;
import com.yh.toy_pj.domain.asset.AssetType;

public record AssetSearchCondition(AssetStatus status, AssetType type, Long assignedUserId, String keyword) {

    public AssetSearchCondition withAssignedUserId(Long userId) {
        return new AssetSearchCondition(status, type, userId, keyword);
    }
}
