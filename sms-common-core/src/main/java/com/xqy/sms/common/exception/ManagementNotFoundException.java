package com.xqy.sms.common.exception;

public class ManagementNotFoundException extends RuntimeException {
    public ManagementNotFoundException() { super("记录不存在或不属于当前范围"); }
}
