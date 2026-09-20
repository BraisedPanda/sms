package com.xqy.sms.web.interfaces.rest;

import com.xqy.sms.ai.api.service.AiRunAccessDeniedException;
import com.xqy.sms.ai.api.service.AiRunNotFoundException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ApiExceptionHandlerTest {
    private final ApiExceptionHandler handler = new ApiExceptionHandler();

    @Test
    void mapsInvalidInputTo400() {
        assertEquals(400, handler.badRequest(new IllegalArgumentException("bad input")).getStatus());
    }

    @Test
    void mapsRunOwnershipTo403() {
        assertEquals(403, handler.forbidden(new AiRunAccessDeniedException()).getStatus());
    }

    @Test
    void mapsMissingRunTo404() {
        assertEquals(404, handler.notFound(new AiRunNotFoundException("run-1")).getStatus());
    }
}
