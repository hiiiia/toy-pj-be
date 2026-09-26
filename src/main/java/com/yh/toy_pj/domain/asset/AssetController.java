package com.yh.toy_pj.domain.asset;

import com.yh.toy_pj.domain.asset.dto.AssetAssignRequest;
import com.yh.toy_pj.domain.asset.dto.AssetCreateRequest;
import com.yh.toy_pj.domain.asset.dto.AssetResponse;
import com.yh.toy_pj.domain.asset.dto.AssetSearchCondition;
import com.yh.toy_pj.domain.asset.dto.AssetUpdateRequest;
import com.yh.toy_pj.global.common.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Asset", description = "IT 자산 관리")
@RestController
@RequestMapping("/api/assets")
@RequiredArgsConstructor
public class AssetController {

    private final AssetService assetService;

    @Operation(summary = "자산 등록", description = "신규 자산은 '재고(AVAILABLE)' 상태로 등록된다.")
    @PostMapping
    public ResponseEntity<AssetResponse> register(@Valid @RequestBody AssetCreateRequest request) {
        AssetResponse response = assetService.register(request);
        return ResponseEntity.created(URI.create("/api/assets/" + response.id())).body(response);
    }

    @Operation(summary = "자산 목록 검색", description = "status, type, assignedUserId, keyword(이름/시리얼) 조건과 페이징(page, size, sort)을 지원한다.")
    @GetMapping
    public PageResponse<AssetResponse> search(@ModelAttribute AssetSearchCondition condition,
                                              @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        return assetService.search(condition, pageable);
    }

    @Operation(summary = "자산 단건 조회")
    @GetMapping("/{id}")
    public AssetResponse get(@PathVariable Long id) {
        return assetService.get(id);
    }

    @Operation(summary = "자산 정보 수정 (부분 수정)")
    @PatchMapping("/{id}")
    public AssetResponse update(@PathVariable Long id, @Valid @RequestBody AssetUpdateRequest request) {
        return assetService.update(id, request);
    }

    @Operation(summary = "자산 배정", description = "재고 상태의 자산을 사용자에게 배정한다.")
    @PostMapping("/{id}/assign")
    public AssetResponse assign(@PathVariable Long id, @Valid @RequestBody AssetAssignRequest request) {
        return assetService.assign(id, request.userId());
    }

    @Operation(summary = "자산 반납")
    @PostMapping("/{id}/return")
    public AssetResponse returnAsset(@PathVariable Long id) {
        return assetService.returnAsset(id);
    }

    @Operation(summary = "점검 시작")
    @PostMapping("/{id}/repair")
    public AssetResponse startRepair(@PathVariable Long id) {
        return assetService.startRepair(id);
    }

    @Operation(summary = "점검 완료")
    @PostMapping("/{id}/repair/complete")
    public AssetResponse completeRepair(@PathVariable Long id) {
        return assetService.completeRepair(id);
    }

    @Operation(summary = "자산 폐기")
    @PostMapping("/{id}/dispose")
    public AssetResponse dispose(@PathVariable Long id) {
        return assetService.dispose(id);
    }

    @Operation(summary = "자산 삭제", description = "잘못 등록된 자산 정리용. 사용중이거나 티켓 이력이 있으면 삭제할 수 없다.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        assetService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
