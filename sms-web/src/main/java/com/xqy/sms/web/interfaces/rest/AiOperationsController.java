package com.xqy.sms.web.interfaces.rest;

import com.xqy.sms.ai.api.service.AiOperationsService;
import com.xqy.sms.ai.api.service.AiKnowledgeCommandService;
import com.xqy.sms.common.dto.ApiResponse;
import com.xqy.sms.common.dto.PageResult;
import com.xqy.sms.web.application.auth.RpcCaller;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AiOperationsController {
    @DubboReference(check = false) private AiOperationsService queries;
    @DubboReference(check = false) private AiKnowledgeCommandService knowledge;
    private final RpcCaller caller;
    public AiOperationsController(RpcCaller caller) { this.caller = caller; }
    @GetMapping("/manage/{resource}")
    public ApiResponse<PageResult> list(@PathVariable String resource, @RequestParam Map<String, String> filters,
                                       @RequestParam(defaultValue="1") int current, @RequestParam(defaultValue="20") int size, Authentication auth) {
        return ApiResponse.success(queries.list(resource, filters, current, size, caller.context(auth)));
    }
    @GetMapping("/conversations/{id}/messages")
    public ApiResponse<PageResult> messages(@PathVariable Long id, @RequestParam(defaultValue="1") int current,
                                           @RequestParam(defaultValue="100") int size, Authentication auth) {
        return ApiResponse.success(queries.messages(id, current, size, caller.context(auth)));
    }
    @GetMapping("/runs/{runId}/snapshot")
    public ApiResponse<Map<String, Object>> snapshot(@PathVariable String runId, Authentication auth) {
        Map<String, Object> delivery = queries.delivery(runId, caller.context(auth));
        delivery.remove("streamKey"); return ApiResponse.success(delivery);
    }
    @PostMapping("/knowledge/versions/{id}/activate")
    public ApiResponse<Void> activate(@PathVariable Long id, @RequestBody IndexRevision input, Authentication auth) {
        knowledge.activateIndex(id, input.indexRevision(), caller.context(auth)); return ApiResponse.success(null);
    }
    public record IndexRevision(int indexRevision) { }
}
