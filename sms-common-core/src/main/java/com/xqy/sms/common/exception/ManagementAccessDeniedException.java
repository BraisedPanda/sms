package com.xqy.sms.common.exception;

public class ManagementAccessDeniedException extends RuntimeException {
    public ManagementAccessDeniedException() { super("Access denied"); }
}
