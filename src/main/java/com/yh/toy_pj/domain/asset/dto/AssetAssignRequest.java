package com.yh.toy_pj.domain.asset.dto;

import jakarta.validation.constraints.NotNull;

public record AssetAssignRequest(@NotNull Long userId) {
}
