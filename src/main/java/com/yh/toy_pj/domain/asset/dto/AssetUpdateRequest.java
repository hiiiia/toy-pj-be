package com.yh.toy_pj.domain.asset.dto;

import com.yh.toy_pj.domain.asset.AssetType;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/** 부분 수정 요청. null 인 필드는 변경하지 않는다. */
public record AssetUpdateRequest(
        @Size(max = 100) @Pattern(regexp = ".*\\S.*", message = "공백만 입력할 수 없습니다.") String name,
        AssetType type,
        @PastOrPresent LocalDate purchasedAt,
        @Size(max = 500) String memo
) {
}
