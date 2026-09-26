package com.yh.toy_pj.global.common;

/**
 * API 에서는 영문 코드(name)로 주고받고, 화면 표시용 한글 라벨을 함께 제공하기 위한 공통 인터페이스.
 */
public interface CodeEnum {

    String name();

    String getLabel();
}
