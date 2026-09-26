package com.yh.toy_pj.domain.asset;

import com.yh.toy_pj.auth.AuthUser;
import com.yh.toy_pj.domain.asset.dto.AssetCreateRequest;
import com.yh.toy_pj.domain.asset.dto.AssetResponse;
import com.yh.toy_pj.domain.asset.dto.AssetSearchCondition;
import com.yh.toy_pj.domain.asset.dto.AssetUpdateRequest;
import com.yh.toy_pj.domain.ticket.TicketRepository;
import com.yh.toy_pj.domain.user.User;
import com.yh.toy_pj.domain.user.UserService;
import com.yh.toy_pj.global.common.PageResponse;
import com.yh.toy_pj.global.error.BusinessException;
import com.yh.toy_pj.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AssetService {

    private final AssetRepository assetRepository;
    private final TicketRepository ticketRepository;
    private final UserService userService;

    @Transactional
    public AssetResponse register(AssetCreateRequest request) {
        if (assetRepository.existsBySerialNumber(request.serialNumber())) {
            throw new BusinessException(ErrorCode.DUPLICATE_SERIAL_NUMBER,
                    "이미 등록된 시리얼 번호입니다: " + request.serialNumber());
        }
        Asset asset = Asset.register(request.name(), request.type(), request.serialNumber(),
                request.purchasedAt(), request.memo());
        return AssetResponse.from(assetRepository.save(asset));
    }

    /** 일반 사용자는 본인에게 배정된 자산만 조회된다. */
    public PageResponse<AssetResponse> search(AssetSearchCondition condition, Pageable pageable, AuthUser me) {
        AssetSearchCondition scoped = me.isAdmin() ? condition : condition.withAssignedUserId(me.id());
        return PageResponse.from(assetRepository.findAll(AssetSpecs.of(scoped), pageable).map(AssetResponse::from));
    }

    public AssetResponse get(Long id, AuthUser me) {
        Asset asset = getAsset(id);
        boolean mine = asset.getAssignedUser() != null && asset.getAssignedUser().getId().equals(me.id());
        if (!me.isAdmin() && !mine) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "본인에게 배정된 자산만 조회할 수 있습니다.");
        }
        return AssetResponse.from(asset);
    }

    @Transactional
    public AssetResponse update(Long id, AssetUpdateRequest request) {
        Asset asset = getAsset(id);
        asset.updateInfo(request.name(), request.type(), request.purchasedAt(), request.memo());
        return AssetResponse.from(asset);
    }

    @Transactional
    public AssetResponse assign(Long id, Long userId) {
        Asset asset = getAsset(id);
        User user = userService.getUser(userId);
        asset.assignTo(user);
        return AssetResponse.from(asset);
    }

    @Transactional
    public AssetResponse returnAsset(Long id) {
        Asset asset = getAsset(id);
        asset.returnAsset();
        return AssetResponse.from(asset);
    }

    @Transactional
    public AssetResponse startRepair(Long id) {
        Asset asset = getAsset(id);
        asset.startRepair();
        return AssetResponse.from(asset);
    }

    @Transactional
    public AssetResponse completeRepair(Long id) {
        Asset asset = getAsset(id);
        asset.completeRepair();
        return AssetResponse.from(asset);
    }

    @Transactional
    public AssetResponse dispose(Long id) {
        Asset asset = getAsset(id);
        asset.dispose();
        return AssetResponse.from(asset);
    }

    @Transactional
    public void delete(Long id) {
        Asset asset = getAsset(id);
        asset.validateDeletable();
        if (ticketRepository.existsByAssetId(id)) {
            throw new BusinessException(ErrorCode.ASSET_HAS_TICKETS);
        }
        assetRepository.delete(asset);
    }

    /** 다른 도메인 서비스에서 자산 엔티티가 필요할 때 사용 (존재하지 않으면 ASSET_NOT_FOUND) */
    public Asset getAsset(Long id) {
        return assetRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ASSET_NOT_FOUND, "자산을 찾을 수 없습니다. id=" + id));
    }
}
