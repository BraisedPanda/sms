package com.xqy.sms.system.api.service;

public class SystemAuthenticationException extends RuntimeException {
    public SystemAuthenticationException() { super("invalid credentials, session, or token"); }
}
