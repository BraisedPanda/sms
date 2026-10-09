package com.xqy.sms.web.application.auth;

import com.xqy.sms.common.security.rpc.InternalCallContext;
import com.xqy.sms.common.security.rpc.InternalCallSigner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import java.util.UUID;

@Component
public class RpcCaller {
    private final InternalCallSigner signer;
    public RpcCaller(@Value("${sms.internal-rpc.secret}") String secret) { signer = new InternalCallSigner(secret); }
    public InternalCallContext context(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthService.AuthenticatedUser user)) throw new AccessDeniedException("Unauthenticated");
        return signer.sign("sms-web-bff", user.tenantId(), user.userId(), user.sessionId(), UUID.randomUUID().toString());
    }
}
