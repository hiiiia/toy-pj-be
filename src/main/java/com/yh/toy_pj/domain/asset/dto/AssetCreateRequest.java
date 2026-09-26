package com.yh.toy_pj.domain.asset.dto;

import com.yh.toy_pj.domain.asset.AssetType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record AssetCreateRequest(
        @NotBlank @Size(max = 100) String name,
        @NotNull AssetType type,
        @NotBlank @Size(max = 50) String serialNumber,
        @PastOrPresent LocalDate purchasedAt,
        @Size(max = 500) String memo
) {
}
