package com.yh.toy_pj.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

class TemporaryPasswordGeneratorTest {

    private final TemporaryPasswordGenerator generator = new TemporaryPasswordGenerator();

    @RepeatedTest(50)
    @DisplayName("임시 비밀번호는 항상 비밀번호 규칙(영문+숫자, 8자 이상)을 만족하고 헷갈리는 문자가 없다")
    void satisfiesPolicy() {
        String password = generator.generate();

        assertThat(password).matches(PasswordPolicy.REGEX).hasSize(12).doesNotContainPattern("[0O1lI]");
    }

    @Test
    @DisplayName("매번 다른 값이 생성된다")
    void unique() {
        Set<String> generated = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            generated.add(generator.generate());
        }
        assertThat(generated).hasSize(1000);
    }
}
