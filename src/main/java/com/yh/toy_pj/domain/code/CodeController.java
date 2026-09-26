package com.yh.toy_pj.domain.code;

import com.yh.toy_pj.domain.asset.AssetStatus;
import com.yh.toy_pj.domain.asset.AssetType;
import com.yh.toy_pj.domain.ticket.ClassificationSource;
import com.yh.toy_pj.domain.ticket.TicketCategory;
import com.yh.toy_pj.domain.ticket.TicketPriority;
import com.yh.toy_pj.domain.ticket.TicketStatus;
import com.yh.toy_pj.domain.user.UserRole;
import com.yh.toy_pj.global.common.CodeEnum;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 프론트엔드 드롭다운/뱃지에 사용할 코드-라벨 목록. 코드 값이 바뀌어도 프론트엔드를 하드코딩으로 수정할 필요가 없다.
 */
@Tag(name = "Code", description = "공통 코드")
@RestController
@RequestMapping("/api/codes")
public class CodeController {

    private static final Map<String, List<CodeResponse>> CODES = buildCodes();

    @Operation(summary = "공통 코드 전체 조회")
    @GetMapping
    public Map<String, List<CodeResponse>> getCodes() {
        return CODES;
    }

    private static Map<String, List<CodeResponse>> buildCodes() {
        Map<String, List<CodeResponse>> codes = new LinkedHashMap<>();
        codes.put("ticketStatus", toCodes(TicketStatus.values()));
        codes.put("ticketPriority", toCodes(TicketPriority.values()));
        codes.put("ticketCategory", toCodes(TicketCategory.values()));
        codes.put("classificationSource", toCodes(ClassificationSource.values()));
        codes.put("assetStatus", toCodes(AssetStatus.values()));
        codes.put("assetType", toCodes(AssetType.values()));
        codes.put("userRole", toCodes(UserRole.values()));
        return Collections.unmodifiableMap(codes);
    }

    private static List<CodeResponse> toCodes(CodeEnum[] values) {
        return Arrays.stream(values).map(v -> new CodeResponse(v.name(), v.getLabel())).toList();
    }

    public record CodeResponse(String code, String label) {
    }
}
