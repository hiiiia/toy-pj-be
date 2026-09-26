package com.yh.toy_pj.global.common;

/**
 * LIKE 검색용 패턴. 사용자가 입력한 %, _ 는 와일드카드가 아니라 글자 그대로 찾도록 이스케이프한다.
 * (이스케이프하지 않으면 "%" 한 글자 검색이 전체 목록을, "_" 가 아무 글자 하나를 매칭한다.)
 */
public final class LikePatterns {

    /** cb.like(expr, pattern, ESCAPE) 로 함께 넘겨야 한다. */
    public static final char ESCAPE = '\\';

    private LikePatterns() {
    }

    /** "포함" 검색 패턴: 앞뒤 공백 제거 + 소문자 + 특수문자 이스케이프 + %...% */
    public static String containsIgnoreCase(String keyword) {
        String escaped = keyword.trim().toLowerCase()
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return "%" + escaped + "%";
    }
}
