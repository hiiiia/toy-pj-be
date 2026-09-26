package com.yh.toy_pj.ai;

import com.yh.toy_pj.global.error.BusinessException;
import com.yh.toy_pj.global.error.ErrorCode;

public class AiException extends BusinessException {

    public AiException(String message) {
        super(ErrorCode.AI_UNAVAILABLE, message);
    }

    public AiException(String message, Throwable cause) {
        super(ErrorCode.AI_UNAVAILABLE, message, cause);
    }
}
