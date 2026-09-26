package com.yh.toy_pj.domain.asset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.yh.toy_pj.domain.user.User;
import com.yh.toy_pj.global.error.BusinessException;
import com.yh.toy_pj.global.error.ErrorCode;
import com.yh.toy_pj.support.Fixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AssetTest {

    @Test
    @DisplayName("신규 자산은 재고 상태로 등록된다")
    void registeredAsAvailable() {
        assertThat(Fixtures.laptop("SN-1").getStatus()).isEqualTo(AssetStatus.AVAILABLE);
    }

    @Test
    @DisplayName("재고 자산을 배정하면 사용중이 되고, 반납하면 다시 재고가 된다")
    void assignAndReturn() {
        Asset asset = Fixtures.laptop("SN-1");
        User user = Fixtures.employee();

        asset.assignTo(user);
        assertThat(asset.getStatus()).isEqualTo(AssetStatus.IN_USE);
        assertThat(asset.getAssignedUser()).isEqualTo(user);

        asset.returnAsset();
        assertThat(asset.getStatus()).isEqualTo(AssetStatus.AVAILABLE);
        assertThat(asset.getAssignedUser()).isNull();
    }

    @Test
    @DisplayName("이미 사용중인 자산은 다른 사람에게 배정할 수 없다")
    void cannotAssignInUseAsset() {
        Asset asset = Fixtures.laptop("SN-1");
        asset.assignTo(Fixtures.employee());

        assertThatThrownBy(() -> asset.assignTo(Fixtures.admin()))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_ASSET_STATE);
    }

    @Test
    @DisplayName("사용중인 자산을 점검 후 완료하면 원래 사용자에게 사용중 상태로 돌아간다")
    void repairKeepsAssignee() {
        Asset asset = Fixtures.laptop("SN-1");
        User user = Fixtures.employee();
        asset.assignTo(user);

        asset.startRepair();
        assertThat(asset.getStatus()).isEqualTo(AssetStatus.REPAIR);

        asset.completeRepair();
        assertThat(asset.getStatus()).isEqualTo(AssetStatus.IN_USE);
        assertThat(asset.getAssignedUser()).isEqualTo(user);
    }

    @Test
    @DisplayName("사용중인 자산은 폐기할 수 없다")
    void cannotDisposeInUseAsset() {
        Asset asset = Fixtures.laptop("SN-1");
        asset.assignTo(Fixtures.employee());

        assertThatThrownBy(asset::dispose)
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("반납");
    }

    @Test
    @DisplayName("부분 수정 시 null 필드는 기존 값을 유지한다")
    void partialUpdate() {
        Asset asset = Fixtures.laptop("SN-1");

        asset.updateInfo("MacBook Air", null, null, "메모");

        assertThat(asset.getName()).isEqualTo("MacBook Air");
        assertThat(asset.getType()).isEqualTo(AssetType.LAPTOP);
        assertThat(asset.getMemo()).isEqualTo("메모");
    }
}
