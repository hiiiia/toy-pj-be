package com.yh.toy_pj.auth;

import java.security.SecureRandom;
import org.springframework.stereotype.Component;

/**
 * 관리자 초기화용 임시 비밀번호 생성기.
 * 비밀번호 규칙(영문+숫자 포함)을 항상 만족하고, 헷갈리기 쉬운 문자(0/O, 1/l/I)는 제외한다.
 */
@Component
public class TemporaryPasswordGenerator {

    private static final String LETTERS = "abcdefghjkmnpqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String DIGITS = "23456789";
    private static final String ALL = LETTERS + DIGITS;
    private static final int LENGTH = 12;

    private final SecureRandom random = new SecureRandom();

    public String generate() {
        char[] chars = new char[LENGTH];
        chars[0] = pick(LETTERS);
        chars[1] = pick(DIGITS);
        for (int i = 2; i < LENGTH; i++) {
            chars[i] = pick(ALL);
        }
        // 앞 두 자리가 항상 "문자+숫자"가 되지 않도록 섞는다 (Fisher-Yates)
        for (int i = LENGTH - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            char tmp = chars[i];
            chars[i] = chars[j];
            chars[j] = tmp;
        }
        return new String(chars);
    }

    private char pick(String source) {
        return source.charAt(random.nextInt(source.length()));
    }
}
