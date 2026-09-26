package com.yh.toy_pj.auth;

/**
 * 비밀번호 규칙: 영문과 숫자를 각각 1자 이상 포함한 8~64자 (공백 제외 ASCII 문자).
 * BCrypt 는 72바이트까지만 사용하므로 한글 등 멀티바이트 문자로 길이가 초과되지 않도록 ASCII 로 제한한다.
 */
public final class PasswordPolicy {

    public static final String REGEX = "^(?=.*[A-Za-z])(?=.*\\d)[\\x21-\\x7E]{8,64}$";
    public static final String MESSAGE = "비밀번호는 영문과 숫자를 포함해 8~64자여야 합니다. (공백·한글 불가)";

    private PasswordPolicy() {
    }
}
