package com.xqy.sms.web.interfaces.rest;

import com.alibaba.com.caucho.hessian.io.Hessian2Input;
import com.alibaba.com.caucho.hessian.io.Hessian2Output;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xqy.sms.ai.api.model.AiChatCommand;
import com.xqy.sms.ai.api.model.AiRunView;
import com.xqy.sms.ai.api.model.KnowledgeIngestionCommand;
import com.xqy.sms.ai.api.model.KnowledgeIngestionResult;
import com.xqy.sms.common.security.rpc.InternalCallContext;
import com.xqy.sms.common.security.rpc.InternalCallSigner;
import com.xqy.sms.system.api.model.SystemAuthModels;
import com.xqy.sms.common.dto.PageResult;
import com.xqy.sms.common.dto.ManagementRows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.beans.Introspector;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class RpcDtoSerializationTest {
    private static final String SECRET = "01234567890123456789012345678901";

    static Stream<Object> transportObjects() {
        InternalCallContext context = new InternalCallSigner(SECRET).sign("sms-web-bff", "tenant-1", 10L, 20L, "request-1");
        SystemAuthModels.Button button = new SystemAuthModels.Button("新增", "add");
        SystemAuthModels.User user = new SystemAuthModels.User(10L, "Admin", "", "管理员", null, "admin@example.com",
                null, "ENABLED", List.of("R_SUPER"), "bootstrap", "2026-10-09", null, null);
        SystemAuthModels.Menu menu = new SystemAuthModels.Menu(1303L, 1301L, "menu", "Menus", "/system/menu", null,
                "menus.system.menu", "ri:menu-line", 10, true, true, false, false, null, false, List.of(button));
        menu.setActivePath("/system/menu");
        AiChatCommand chat = new AiChatCommand(10L, 20L, "你好", "balanced", null, "stream-1", "tenant-1", context);
        chat.setConversationId(30L);
        KnowledgeIngestionResult queued = new KnowledgeIngestionResult(10, 0, 1);
        queued.setJobIds(List.of("9123456789012345678"));
        queued.setStatus("QUEUED");
        return Stream.of(
                new SystemAuthModels.TokenPair("access", "refresh"),
                new SystemAuthModels.Principal(10L, 20L, "token-1", "tenant-1", List.of("R_SUPER"), List.of("user:read")),
                new SystemAuthModels.UserInfo(List.of("add"), List.of("R_SUPER"), 10L, "Admin", null, null),
                menu, button, new SystemAuthModels.UserPage(List.of(user), 1, 10, 1), user,
                chat,
                new AiRunView("run-1", "RUNNING", 1, LocalDateTime.of(2026, 10, 9, 12, 0), null),
                new KnowledgeIngestionCommand(1L, 2L, 10, 1, "tenant-1", context),
                queued, context);
    }

    @ParameterizedTest
    @MethodSource("transportObjects")
    void hessianRoundTripPreservesBeanPropertiesAndCompatibilityAccessors(Object source) throws Exception {
        assertFalse(source.getClass().isRecord());
        Object empty = source.getClass().getConstructor().newInstance();
        Object copy = roundTrip(source);
        ObjectMapper json = new ObjectMapper().findAndRegisterModules();
        assertEquals(json.valueToTree(source), json.valueToTree(copy));
        for (var property : Introspector.getBeanInfo(source.getClass(), Object.class).getPropertyDescriptors()) {
            assertNotNull(property.getWriteMethod(), property.getName());
            Object expected = property.getReadMethod().invoke(source);
            property.getWriteMethod().invoke(empty, expected);
            assertEquals(expected, property.getReadMethod().invoke(empty));
            assertEquals(property.getReadMethod().invoke(copy),
                    source.getClass().getMethod(property.getName()).invoke(copy));
        }
    }

    @Test
    void signedCallerContextStillVerifiesAfterHessianTransport() throws Exception {
        InternalCallSigner signer = new InternalCallSigner(SECRET);
        InternalCallContext context = signer.sign("sms-web-bff", "tenant-1", 10L, 20L, "request-1");
        signer.verify((InternalCallContext) roundTrip(context), "sms-web-bff");
    }

    @Test
    void managementPagesPreserveNestedRowsAndLargeStringIdentifiers() throws Exception {
        Map<String, Object> row = ManagementRows.publicRow(Map.of("id", 9123456789012345678L,
                "document_version_id", 9123456789012345601L, "name", "中文😀",
                "create_time", java.sql.Timestamp.valueOf("2026-10-09 12:00:00"),
                "metadata", Map.of("roleIds", List.of("1101", "1102"), "strategy", "PARAGRAPH")));
        PageResult source = new PageResult(List.of(row), 2, 20, 30);
        PageResult copy = (PageResult) roundTrip(source);
        assertEquals(source.getRecords(), copy.getRecords());
        assertEquals("9123456789012345678", copy.getRecords().getFirst().get("id"));
        assertEquals(2, copy.getCurrent());
        assertEquals(20, copy.getSize());
        assertEquals(30, copy.getTotal());
    }

    private Object roundTrip(Object value) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        Hessian2Output output = new Hessian2Output(bytes);
        output.writeObject(value);
        output.flush();
        Hessian2Input input = new Hessian2Input(new ByteArrayInputStream(bytes.toByteArray()));
        return input.readObject(value.getClass());
    }
}
