package com.xqy.sms.common.security.rpc;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InternalCallSignerTest {
    private static final String SECRET = "01234567890123456789012345678901";

    @Test
    void acceptsSignedExpectedCaller() {
        InternalCallSigner signer = new InternalCallSigner(SECRET);
        InternalCallContext context = signer.sign("sms-web-bff", "default", 1L, 2L, "request-1");
        assertDoesNotThrow(() -> signer.verify(context, "sms-web-bff"));
    }

    @Test
    void rejectsWrongCaller() {
        InternalCallSigner signer = new InternalCallSigner(SECRET);
        InternalCallContext context = signer.sign("sms-web-bff", "default", 1L, 2L, "request-1");
        assertThrows(InternalCallSigner.InternalCallAuthenticationException.class,
                () -> signer.verify(context, "sms-ai-provider"));
    }
}
