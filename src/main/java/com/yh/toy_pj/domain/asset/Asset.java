package com.yh.toy_pj.domain.asset;

import com.yh.toy_pj.domain.user.User;
import com.yh.toy_pj.global.common.BaseTimeEntity;
import com.yh.toy_pj.global.error.BusinessException;
import com.yh.toy_pj.global.error.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Version;
import java.time.LocalDate;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * IT 자산. 상태 변경은 setter 대신 의도가 드러나는 도메인 메서드(assignTo, returnAsset ...)로만 가능하다.
 *
 * <pre>
 *  AVAILABLE --assignTo--> IN_USE --returnAsset--> AVAILABLE
 *      |                     |
 *      +----startRepair------+--> REPAIR --completeRepair--> (배정자 있으면 IN_USE, 없으면 AVAILABLE)
 *  AVAILABLE/REPAIR --dispose--> DISPOSED (종료 상태)
 * </pre>
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Asset extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AssetType type;

    @Column(nullable = false, unique = true, length = 50)
    private String serialNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AssetStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_user_id")
    private User assignedUser;

    private LocalDate purchasedAt;

    @Column(length = 500)
    private String memo;

    /** 낙관적 락: 여러 담당자가 동시에 같은 대상을 수정할 때 나중 요청이 앞선 변경을 덮어쓰지 않도록 한다. */
    @Version
    private Long version;

    private Asset(String name, AssetType type, String serialNumber, LocalDate purchasedAt, String memo) {
        this.name = name;
        this.type = type;
        this.serialNumber = serialNumber;
        this.purchasedAt = purchasedAt;
        this.memo = memo;
        this.status = AssetStatus.AVAILABLE;
    }

    public static Asset register(String name, AssetType type, String serialNumber, LocalDate purchasedAt, String memo) {
        return new Asset(name, type, serialNumber, purchasedAt, memo);
    }

    /** null 인 값은 변경하지 않는다 (부분 수정). */
    public void updateInfo(String name, AssetType type, LocalDate purchasedAt, String memo) {
        if (name != null) this.name = name;
        if (type != null) this.type = type;
        if (purchasedAt != null) this.purchasedAt = purchasedAt;
        if (memo != null) this.memo = memo;
    }

    public void assignTo(User user) {
        Objects.requireNonNull(user, "user");
        requireStatus(AssetStatus.AVAILABLE, "재고 상태의 자산만 배정할 수 있습니다.");
        this.assignedUser = user;
        this.status = AssetStatus.IN_USE;
    }

    public void returnAsset() {
        requireStatus(AssetStatus.IN_USE, "사용중인 자산만 반납할 수 있습니다.");
        this.assignedUser = null;
        this.status = AssetStatus.AVAILABLE;
    }

    public void startRepair() {
        if (status != AssetStatus.AVAILABLE && status != AssetStatus.IN_USE) {
            throw invalidState("재고 또는 사용중인 자산만 점검을 시작할 수 있습니다.");
        }
        this.status = AssetStatus.REPAIR;
    }

    public void completeRepair() {
        requireStatus(AssetStatus.REPAIR, "점검중인 자산만 점검 완료 처리할 수 있습니다.");
        this.status = (assignedUser != null) ? AssetStatus.IN_USE : AssetStatus.AVAILABLE;
    }

    public void dispose() {
        if (status == AssetStatus.IN_USE || status == AssetStatus.DISPOSED) {
            throw invalidState("사용중이거나 이미 폐기된 자산은 폐기할 수 없습니다. 먼저 반납 처리하세요.");
        }
        this.assignedUser = null;
        this.status = AssetStatus.DISPOSED;
    }

    public void validateDeletable() {
        if (status == AssetStatus.IN_USE) {
            throw invalidState("사용중인 자산은 삭제할 수 없습니다.");
        }
    }

    private void requireStatus(AssetStatus expected, String message) {
        if (this.status != expected) {
            throw invalidState(message);
        }
    }

    private BusinessException invalidState(String message) {
        return new BusinessException(ErrorCode.INVALID_ASSET_STATE,
                message + " (현재 상태: " + status.getLabel() + ")");
    }
}
