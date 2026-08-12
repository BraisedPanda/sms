package com.xqy.sms.common.exception;

import com.xqy.sms.common.dto.CommonCode;

/**
 * 业务异常
 */
public class BusinessException extends RuntimeException {
    private final int code;

    public BusinessException(CommonCode code) {
        super(code.getMessage());
        this.code = code.getCode();
    }

    public BusinessException(CommonCode code, String message) {
        super(message);
        this.code = code.getCode();
    }

    public int getCode() {
        return code;
    }
}
