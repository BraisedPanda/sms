package com.xqy.sms.common.dto;

/**
 * 通用错误码定义
 */
public enum CommonCode {
    SUCCESS(0, "success"),
    UNKNOWN_ERROR(500, "unknown error"),
    NOT_FOUND(404, "not found"),
    INVALID_PARAMETER(400, "invalid parameter");

    private final int code;
    private final String message;

    CommonCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
