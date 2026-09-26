package com.yh.toy_pj.notification;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class SlaMonitorTest {

    @ParameterizedTest(name = "{0}분 → {1}")
    @CsvSource({"0, 1분", "40, 40분", "60, 1시간", "95, 1시간 35분", "1356, 22시간 36분"})
    @DisplayName("남은 시간을 사람이 읽기 쉬운 형태로 표시한다")
    void humanize(long minutes, String expected) {
        assertThat(SlaMonitor.humanize(Duration.ofMinutes(minutes))).isEqualTo(expected);
    }
}
