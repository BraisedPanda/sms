package com.xqy.sms.web.interfaces.rest;

import com.xqy.sms.common.security.rpc.InternalCallSigner;
import com.xqy.sms.ai.api.service.AiRunAccessDeniedException;
import com.xqy.sms.ai.api.service.AiRunNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Stable RFC 9457 error contract for API clients. */
@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail badRequest(IllegalArgumentException error) {
        return problem(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", error.getMessage());
    }

    @ExceptionHandler({AccessDeniedException.class, InternalCallSigner.InternalCallAuthenticationException.class,
            AiRunAccessDeniedException.class})
    ProblemDetail forbidden(RuntimeException error) {
        return problem(HttpStatus.FORBIDDEN, "ACCESS_DENIED", "Access denied");
    }

    @ExceptionHandler(AiRunNotFoundException.class)
    ProblemDetail notFound(AiRunNotFoundException error) {
        return problem(HttpStatus.NOT_FOUND, "RUN_NOT_FOUND", error.getMessage());
    }

    private ProblemDetail problem(HttpStatus status, String code, String detail) {
        ProblemDetail value = ProblemDetail.forStatusAndDetail(status, detail == null ? status.getReasonPhrase() : detail);
        value.setProperty("code", code);
        return value;
    }
}
