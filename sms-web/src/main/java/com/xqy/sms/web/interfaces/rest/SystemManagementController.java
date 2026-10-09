package com.xqy.sms.web.interfaces.rest;

import com.xqy.sms.common.dto.ApiResponse;
import com.xqy.sms.common.dto.PageResult;
import com.xqy.sms.system.api.service.SystemManagementService;
import com.xqy.sms.web.application.auth.RpcCaller;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/system/manage")
public class SystemManagementController {
    @DubboReference(check = false) private SystemManagementService management;
    private final RpcCaller caller;
    public SystemManagementController(RpcCaller caller) { this.caller = caller; }

    @GetMapping("/{resource}")
    public ApiResponse<PageResult> list(@PathVariable String resource, @RequestParam Map<String, String> filters,
                                       @RequestParam(defaultValue="1") int current, @RequestParam(defaultValue="20") int size, Authentication auth) {
        return ApiResponse.success(management.list(resource, filters, current, size, caller.context(auth)));
    }
    @PostMapping("/{resource}")
    public ApiResponse<Map<String, Object>> create(@PathVariable String resource, @RequestBody Map<String, Object> values, Authentication auth) {
        return ApiResponse.success(management.save(resource, null, values, caller.context(auth)));
    }
    @PutMapping("/{resource}/{id}")
    public ApiResponse<Map<String, Object>> update(@PathVariable String resource, @PathVariable Long id, @RequestBody Map<String, Object> values, Authentication auth) {
        return ApiResponse.success(management.save(resource, id, values, caller.context(auth)));
    }
    @DeleteMapping("/{resource}/{id}")
    public ApiResponse<Void> delete(@PathVariable String resource, @PathVariable Long id, Authentication auth) {
        management.delete(resource, id, caller.context(auth)); return ApiResponse.success(null);
    }
    @GetMapping("/{resource}/{id}/assignments")
    public ApiResponse<Map<String, Object>> assignments(@PathVariable String resource, @PathVariable Long id, Authentication auth) {
        return ApiResponse.success(management.assignments(resource, id, caller.context(auth)));
    }
    @PutMapping("/users/{id}/roles")
    public ApiResponse<Void> assignRoles(@PathVariable Long id, @RequestBody UserRoles input, Authentication auth) {
        management.assignUserRoles(id, input.roleIds(), caller.context(auth)); return ApiResponse.success(null);
    }
    @PutMapping("/roles/{id}/permissions")
    public ApiResponse<Void> grant(@PathVariable Long id, @RequestBody RolePermissions input, Authentication auth) {
        management.grantRole(id, input.menuIds(), input.buttonIds(), caller.context(auth)); return ApiResponse.success(null);
    }
    @PostMapping("/sessions/{id}/revoke")
    public ApiResponse<Void> revoke(@PathVariable Long id, Authentication auth) {
        management.revokeSession(id, caller.context(auth)); return ApiResponse.success(null);
    }
    public record UserRoles(List<Long> roleIds) { }
    public record RolePermissions(List<Long> menuIds, List<Long> buttonIds) { }
}
