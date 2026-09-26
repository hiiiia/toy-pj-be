package com.yh.toy_pj.domain.ticket;

import com.yh.toy_pj.global.common.CodeEnum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TicketCategory implements CodeEnum {
    HARDWARE("하드웨어"),
    SOFTWARE("소프트웨어"),
    NETWORK("네트워크"),
    ACCOUNT("계정/권한"),
    ETC("기타");

    private final String label;
}
