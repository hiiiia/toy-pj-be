package com.yh.toy_pj.domain.user;

import static org.assertj.core.api.Assertions.assertThat;

import com.yh.toy_pj.support.Fixtures;
import java.time.Duration;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UserTest {

    private static final LocalDateTime NOW = Fixtures.NOW;
    private static final Duration LOCK = Duration.ofMinutes(15);

    @Test
    @DisplayName("연속 5번 실패하면 15분 동안 잠기고, 실패 횟수는 초기화된다")
    void locksAfterMaxAttempts() {
        User user = Fixtures.employee();

        for (int i = 1; i <= 4; i++) {
            assertThat(user.recordLoginFailure(NOW, 5, LOCK)).isFalse();
        }
        assertThat(user.recordLoginFailure(NOW, 5, LOCK)).isTrue();

        assertThat(user.isLocked(NOW.plusMinutes(14))).isTrue();
        assertThat(user.isLocked(NOW.plusMinutes(15))).isFalse(); // 잠금 시간이 지나면 자동 해제
        assertThat(user.getFailedLoginCount()).isZero();
    }

    @Test
    @DisplayName("중간에 로그인에 성공하면 실패 횟수가 다시 0 부터 센다")
    void successResetsCount() {
        User user = Fixtures.employee();
        user.recordLoginFailure(NOW, 5, LOCK);
        user.recordLoginFailure(NOW, 5, LOCK);

        user.recordLoginSuccess();

        assertThat(user.getFailedLoginCount()).isZero();
    }

    @Test
    @DisplayName("관리자가 비밀번호를 초기화하면 잠금이 풀리고, 다음 로그인 때 변경을 요구한다")
    void resetPassword() {
        User user = Fixtures.employee();
        for (int i = 0; i < 5; i++) {
            user.recordLoginFailure(NOW, 5, LOCK);
        }

        user.resetPassword("{bcrypt}temp", NOW);

        assertThat(user.isLocked(NOW)).isFalse();
        assertThat(user.isMustChangePassword()).isTrue();

        user.changePassword("{bcrypt}new", NOW.plusMinutes(1));
        assertThat(user.isMustChangePassword()).isFalse();
        assertThat(user.getPasswordChangedAt()).isEqualTo(NOW.plusMinutes(1));
    }
}
