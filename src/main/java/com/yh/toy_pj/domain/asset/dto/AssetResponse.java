package com.yh.toy_pj.domain.asset.dto;

import com.yh.toy_pj.domain.asset.Asset;
import com.yh.toy_pj.domain.asset.AssetStatus;
import com.yh.toy_pj.domain.asset.AssetType;
import com.yh.toy_pj.domain.user.User;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record AssetResponse(
        Long id,
        String name,
        AssetType type,
        String serialNumber,
        AssetStatus status,
        Long assignedUserId,
        String assignedUserName,
        LocalDate purchasedAt,
        String memo,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static AssetResponse from(Asset asset) {
        User user = asset.getAssignedUser();
        return new AssetResponse(
                asset.getId(),
                asset.getName(),
                asset.getType(),
                asset.getSerialNumber(),
                asset.getStatus(),
                user != null ? user.getId() : null,
                user != null ? user.getName() : null,
                asset.getPurchasedAt(),
                asset.getMemo(),
                asset.getCreatedAt(),
                asset.getUpdatedAt()
        );
    }
}
