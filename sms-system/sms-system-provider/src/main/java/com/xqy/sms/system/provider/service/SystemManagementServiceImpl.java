package com.xqy.sms.system.provider.service;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.xqy.sms.common.dto.ManagementRows;
import com.xqy.sms.common.dto.PageResult;
import com.xqy.sms.common.exception.ManagementAccessDeniedException;
import com.xqy.sms.common.exception.ManagementConflictException;
import com.xqy.sms.common.exception.ManagementNotFoundException;
import com.xqy.sms.common.security.rpc.InternalCallContext;
import com.xqy.sms.common.security.rpc.InternalCallSigner;
import com.xqy.sms.system.api.service.SystemManagementService;
import com.xqy.sms.system.provider.mapper.SysAuthorizationMapper;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.*;

/** RBAC writes and device sessions live in the identity provider. */
@Service
@DubboService
public class SystemManagementServiceImpl implements SystemManagementService {
    private static final Map<String, String> TABLES = Map.of("users", "sys_user", "roles", "sys_role",
            "menus", "sys_menu", "buttons", "sys_button", "sessions", "sys_user_session");
    private static final Map<String, String> COLUMNS = Map.of(
            "users", "id,tenant_id,username,nickname,real_name,email,phone,avatar,user_type,status,last_login_time,last_login_ip,create_time,update_time",
            "roles", "id,role_code,role_name,description,role_type,status,create_time,update_time",
            "menus", "id,parent_id,path,route_name,component,redirect,title,icon,sort_no,keep_alive,visible,hide_tab,full_page,external_link,iframe_flag,active_path,status",
            "buttons", "id,menu_id,button_name,auth_remark,description,sort_no,status",
            "sessions", "id,tenant_id,user_id,login_ip,user_agent,device_type,login_time,expire_time,refresh_expire_time,status,logout_time");
    private static final Map<String, List<String>> FIELDS = Map.of(
            "users", List.of("username", "nickname", "realName", "email", "phone", "avatar", "status", "password"),
            "roles", List.of("roleCode", "roleName", "description", "status"),
            "menus", List.of("parentId", "path", "routeName", "component", "redirect", "title", "icon", "sortNo", "keepAlive", "visible", "hideTab", "fullPage", "externalLink", "iframeFlag", "activePath", "status"),
            "buttons", List.of("menuId", "buttonName", "authRemark", "description", "sortNo", "status"));
    private final JdbcTemplate jdbc;
    private final SysAuthorizationMapper authorization;
    private final SystemAuthServiceImpl auth;
    private final InternalCallSigner signer;
    private final BCryptPasswordEncoder passwords = new BCryptPasswordEncoder();

    public SystemManagementServiceImpl(JdbcTemplate jdbc, SysAuthorizationMapper authorization,
                                       SystemAuthServiceImpl auth, @Value("${sms.internal-rpc.secret}") String secret) {
        this.jdbc = jdbc; this.authorization = authorization; this.auth = auth; this.signer = new InternalCallSigner(secret);
    }

    @Override
    public void authorize(String authority, InternalCallContext caller) {
        signer.verify(caller, "sms-web-bff");
        Integer valid = jdbc.queryForObject("""
                SELECT COUNT(*) FROM sys_user_session s JOIN sys_user u ON u.id=s.user_id
                WHERE s.id=? AND s.user_id=? AND s.tenant_id=? AND u.tenant_id=s.tenant_id
                  AND s.status='ACTIVE' AND s.expire_time>CURRENT_TIMESTAMP AND u.status='ENABLED'
                """, Integer.class, caller.sessionId(), caller.userId(), caller.tenantId());
        if (valid == null || valid != 1) throw new ManagementAccessDeniedException();
        if ("authenticated".equals(authority) || superUser(caller)) return;
        if (!authorization.findButtonAuthorities(caller.userId()).contains(authority)) throw new ManagementAccessDeniedException();
    }

    @Override
    public PageResult list(String resource, Map<String, String> filters, int current, int size, InternalCallContext caller) {
        String table = table(resource);
        filters = filters == null ? Map.of() : filters;
        boolean ownSessions = "sessions".equals(resource) && !"true".equals(filters.get("all"));
        authorize(ownSessions ? "authenticated" : permission(resource, "read"), caller);
        String where = " WHERE 1=1";
        List<Object> args = new ArrayList<>();
        if ("users".equals(resource) || "sessions".equals(resource)) {
            where += " AND tenant_id=?"; args.add(caller.tenantId());
        }
        if (ownSessions) { where += " AND user_id=?"; args.add(caller.userId()); }
        if (filters.get("status") != null && !filters.get("status").isBlank()) {
            where += " AND status=?"; args.add(filters.get("status"));
        }
        if ("buttons".equals(resource) && filters.get("menuId") != null && !filters.get("menuId").isBlank()) {
            where += " AND menu_id=?"; args.add(ManagementRows.id(filters.get("menuId")));
        }
        String search = ManagementRows.text(filters.get("search"), 128, false);
        if (search != null && !search.isBlank()) {
            String column = switch (resource) { case "users" -> "username"; case "roles" -> "role_name";
                case "menus" -> "title"; case "buttons" -> "button_name"; default -> "user_agent"; };
            where += " AND " + column + " LIKE ?"; args.add("%" + search + "%");
        }
        current = Math.max(1, current); size = Math.max(1, Math.min(200, size));
        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM " + table + where, Long.class, args.toArray());
        args.add(size); args.add(((long) current - 1) * size);
        List<Map<String, Object>> rows = ManagementRows.publicRows(jdbc.queryForList("SELECT " + COLUMNS.get(resource)
                + " FROM " + table + where + " ORDER BY id DESC LIMIT ? OFFSET ?", args.toArray()));
        if ("sessions".equals(resource)) rows.forEach(row -> row.put("currentSession", String.valueOf(caller.sessionId()).equals(row.get("id"))));
        if ("users".equals(resource)) rows.forEach(row -> row.put("roleCodes", authorization.findRoleCodes(Long.valueOf(row.get("id").toString()))));
        return new PageResult(rows, current, size, total == null ? 0 : total);
    }

    @Override
    @Transactional
    public Map<String, Object> save(String resource, Long id, Map<String, Object> input, InternalCallContext caller) {
        authorize(permission(resource, id == null ? "create" : "update"), caller);
        if (!FIELDS.containsKey(resource) || input == null) throw new IllegalArgumentException("不支持的写操作");
        globalWrite(resource, caller);
        Map<String, Object> previous = id == null ? Map.of() : owned(resource, id, caller);
        Map<String, Object> values = validated(resource, input, id == null);
        if ("users".equals(resource) && id != null) protectAdministrator(id, caller, "DISABLED".equals(values.get("status")));
        if ("users".equals(resource) && (Objects.equals(id, caller.userId()) || "SYSTEM".equals(previous.get("user_type")))
                && "DISABLED".equals(values.get("status"))) throw new ManagementConflictException("不能禁用当前用户或内置管理员");
        if ("roles".equals(resource) && "SYSTEM".equals(previous.get("role_type"))) {
            if ((values.containsKey("role_code") && !Objects.equals(values.get("role_code"), previous.get("role_code")))
                    || "DISABLED".equals(values.get("status"))) throw new ManagementConflictException("内置角色不能改名或禁用");
        }
        if ("menus".equals(resource)) validateParent(id, values.get("parent_id"));
        if ("buttons".equals(resource)) {
            Long menuId = input.containsKey("menuId") ? ManagementRows.id(input.get("menuId")) : ((Number) previous.get("menu_id")).longValue();
            requireExists("sys_menu", menuId);
        }
        if (id == null) {
            id = IdWorker.getId(); values.put("id", id); values.put("create_by", String.valueOf(caller.userId()));
            if ("users".equals(resource)) { values.put("tenant_id", caller.tenantId()); values.put("user_type", "NORMAL"); }
            if ("roles".equals(resource)) values.put("role_type", "CUSTOM");
        }
        values.put("modify_by", String.valueOf(caller.userId()));
        try {
            if (previous.isEmpty()) insert(table(resource), values);
            else update(table(resource), id, values);
        } catch (DataIntegrityViolationException error) { throw new ManagementConflictException("名称、路由或授权标识重复，或字段不符合表结构"); }
        if ("menus".equals(resource) || "buttons".equals(resource)) grantSuper(resource, id);
        if ("users".equals(resource) && (values.containsKey("password") || "DISABLED".equals(values.get("status")))) terminateUserSessions(id);
        return ManagementRows.publicRow(jdbc.queryForMap("SELECT " + COLUMNS.get(resource) + " FROM " + table(resource) + " WHERE id=?", id));
    }

    @Override
    @Transactional
    public void delete(String resource, Long id, InternalCallContext caller) {
        authorize(permission(resource, "delete"), caller); globalWrite(resource, caller);
        Map<String, Object> row = owned(resource, id, caller);
        switch (resource) {
            case "users" -> {
                if (Objects.equals(id, caller.userId()) || "SYSTEM".equals(row.get("user_type"))) throw new ManagementConflictException("不能删除当前用户或内置管理员");
                protectAdministrator(id, caller, true);
                terminateUserSessions(id); jdbc.update("DELETE FROM sys_user_role WHERE user_id=?", id);
            }
            case "roles" -> {
                if ("SYSTEM".equals(row.get("role_type"))) throw new ManagementConflictException("不能删除内置角色");
                if (count("SELECT COUNT(*) FROM sys_user_role WHERE role_id=?", id) > 0) throw new ManagementConflictException("请先移除角色的用户关联");
                jdbc.update("DELETE FROM sys_role_button WHERE role_id=?", id); jdbc.update("DELETE FROM sys_role_menu WHERE role_id=?", id);
            }
            case "menus" -> {
                if (count("SELECT COUNT(*) FROM sys_menu WHERE parent_id=?", id) > 0 || count("SELECT COUNT(*) FROM sys_button WHERE menu_id=?", id) > 0)
                    throw new ManagementConflictException("请先删除子菜单和按钮");
                jdbc.update("DELETE FROM sys_role_menu WHERE menu_id=?", id);
            }
            case "buttons" -> jdbc.update("DELETE FROM sys_role_button WHERE button_id=?", id);
            default -> throw new IllegalArgumentException("不支持的删除操作");
        }
        jdbc.update("DELETE FROM " + table(resource) + " WHERE id=?", id);
    }

    @Override
    public Map<String, Object> assignments(String resource, Long id, InternalCallContext caller) {
        authorize(permission(resource, "read"), caller); owned(resource, id, caller);
        if ("users".equals(resource)) return Map.of("roleIds", ids("SELECT role_id FROM sys_user_role WHERE user_id=?", id));
        if ("roles".equals(resource)) return Map.of("menuIds", ids("SELECT menu_id FROM sys_role_menu WHERE role_id=?", id),
                "buttonIds", ids("SELECT button_id FROM sys_role_button WHERE role_id=?", id));
        throw new IllegalArgumentException("不支持的授权对象");
    }

    @Override
    @Transactional
    public void assignUserRoles(Long userId, List<Long> roleIds, InternalCallContext caller) {
        authorize("system:user:grant", caller); owned("users", userId, caller);
        protectAdministrator(userId, caller, false);
        if (Objects.equals(userId, caller.userId())) throw new ManagementConflictException("不能修改当前用户的角色");
        List<Long> roles = validIds(roleIds);
        for (Long id : roles) {
            requireExists("sys_role", id);
            if (!superUser(caller) && "R_SUPER".equals(jdbc.queryForObject("SELECT role_code FROM sys_role WHERE id=?", String.class, id)))
                throw new ManagementAccessDeniedException();
        }
        // Serialize changes to administrator membership before checking the last remaining account.
        Long superRole = jdbc.queryForObject("SELECT id FROM sys_role WHERE role_code='R_SUPER' FOR UPDATE", Long.class);
        if (!roles.contains(superRole) && count("SELECT COUNT(*) FROM sys_user_role WHERE user_id=? AND role_id=?", userId, superRole) > 0) {
            if (!superUser(caller)) throw new ManagementAccessDeniedException();
            if (count("SELECT COUNT(*) FROM sys_user_role ur JOIN sys_user u ON u.id=ur.user_id WHERE ur.role_id=? AND u.status='ENABLED'", superRole) <= 1)
                throw new ManagementConflictException("必须保留至少一个启用的超级管理员");
        }
        jdbc.update("DELETE FROM sys_user_role WHERE user_id=?", userId);
        for (Long role : roles) jdbc.update("INSERT INTO sys_user_role (id,user_id,role_id,create_by,modify_by) VALUES (?,?,?,?,?)",
                IdWorker.getId(), userId, role, caller.userId().toString(), caller.userId().toString());
    }

    @Override
    @Transactional
    public void grantRole(Long roleId, List<Long> menuIds, List<Long> buttonIds, InternalCallContext caller) {
        authorize("system:role:grant", caller); globalWrite("roles", caller);
        Map<String, Object> role = owned("roles", roleId, caller);
        if ("R_SUPER".equals(role.get("role_code"))) throw new ManagementConflictException("超级管理员自动拥有全部权限");
        List<Long> menus = validIds(menuIds), buttons = validIds(buttonIds);
        for (Long menu : menus) requireExists("sys_menu", menu);
        for (Long button : buttons) {
            requireExists("sys_button", button);
            Long menu = jdbc.queryForObject("SELECT menu_id FROM sys_button WHERE id=?", Long.class, button);
            if (!menus.contains(menu)) throw new IllegalArgumentException("按钮对应的菜单必须同时授权");
        }
        for (Long menu : menus) {
            Long parent = jdbc.queryForObject("SELECT parent_id FROM sys_menu WHERE id=?", Long.class, menu);
            if (parent != null && !menus.contains(parent)) throw new IllegalArgumentException("请同时选择菜单的全部父级");
        }
        jdbc.update("DELETE FROM sys_role_button WHERE role_id=?", roleId); jdbc.update("DELETE FROM sys_role_menu WHERE role_id=?", roleId);
        for (Long menu : menus) jdbc.update("INSERT INTO sys_role_menu (id,role_id,menu_id) VALUES (?,?,?)", IdWorker.getId(), roleId, menu);
        for (Long button : buttons) jdbc.update("INSERT INTO sys_role_button (id,role_id,button_id) VALUES (?,?,?)", IdWorker.getId(), roleId, button);
    }

    @Override
    @Transactional
    public void revokeSession(Long sessionId, InternalCallContext caller) {
        authorize("authenticated", caller);
        List<Map<String, Object>> sessions = jdbc.queryForList("SELECT user_id FROM sys_user_session WHERE id=? AND tenant_id=?", sessionId, caller.tenantId());
        if (sessions.isEmpty()) throw new ManagementNotFoundException();
        if (!Objects.equals(((Number) sessions.getFirst().get("user_id")).longValue(), caller.userId())) authorize("system:session:revoke", caller);
        auth.kick(sessionId);
    }

    private Map<String, Object> validated(String resource, Map<String, Object> input, boolean create) {
        Map<String, Object> values = new LinkedHashMap<>();
        for (String field : input.keySet()) if (!FIELDS.get(resource).contains(field)) throw new IllegalArgumentException("不允许写入字段: " + field);
        if (create) { values.put("status", "ENABLED"); if (resource.equals("menus")) { values.put("visible", true); values.put("sort_no", 0); } if (resource.equals("buttons")) values.put("sort_no", 0); }
        for (var entry : input.entrySet()) {
            String field = entry.getKey(); Object value = entry.getValue();
            if ("password".equals(field)) {
                if (value == null || value.toString().isBlank()) { if (create) throw new IllegalArgumentException("新用户必须设置密码"); continue; }
                String password = value.toString();
                if (password.length() < 6 || password.getBytes(StandardCharsets.UTF_8).length > 72) throw new IllegalArgumentException("密码至少 6 个字符且不超过 72 字节");
                value = passwords.encode(password);
            } else if (field.endsWith("Id")) value = value == null || value.toString().isBlank() ? null : ManagementRows.id(value);
            else if ("sortNo".equals(field)) {
                try { value = Integer.valueOf(value.toString()); } catch (RuntimeException error) { throw new IllegalArgumentException("排序必须为整数"); }
            } else if (List.of("keepAlive", "visible", "hideTab", "fullPage", "iframeFlag").contains(field)) {
                if (!(value instanceof Boolean)) throw new IllegalArgumentException("开关字段必须为布尔值");
            } else {
                int maximum = switch (field) { case "username", "nickname", "realName", "roleCode", "roleName" -> 64;
                    case "phone" -> 32; case "email", "title", "icon", "routeName", "buttonName", "authRemark" -> 128;
                    case "description", "avatar", "externalLink" -> 512; case "status" -> 16; default -> 256; };
                value = ManagementRows.text(value, maximum, false);
                if ("status".equals(field) && !List.of("ENABLED", "DISABLED").contains(value)) throw new IllegalArgumentException("无效状态");
            }
            values.put(snake(field), value);
        }
        List<String> required = switch (resource) { case "users" -> List.of("username", "password");
            case "roles" -> List.of("role_code", "role_name"); case "menus" -> List.of("title"); default -> List.of("menu_id", "button_name", "auth_remark"); };
        for (String field : required) if ((create || values.containsKey(field)) && (values.get(field) == null || values.get(field).toString().isBlank()))
            throw new IllegalArgumentException("必填字段不能为空: " + field);
        return values;
    }

    private Map<String, Object> owned(String resource, Long id, InternalCallContext caller) {
        ManagementRows.id(id);
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT * FROM " + table(resource) + " WHERE id=?" + ("users".equals(resource) ? " AND tenant_id=?" : ""),
                "users".equals(resource) ? new Object[]{id, caller.tenantId()} : new Object[]{id});
        if (rows.isEmpty()) throw new ManagementNotFoundException(); return rows.getFirst();
    }
    private void validateParent(Long id, Object value) {
        if (value == null) return;
        Long parent = ManagementRows.id(value); Set<Long> visited = new HashSet<>();
        while (parent != null) {
            if (Objects.equals(parent, id) || !visited.add(parent)) throw new IllegalArgumentException("菜单不能循环引用");
            requireExists("sys_menu", parent);
            parent = jdbc.queryForObject("SELECT parent_id FROM sys_menu WHERE id=?", Long.class, parent);
        }
    }
    private void globalWrite(String resource, InternalCallContext caller) {
        if (!"users".equals(resource) && !superUser(caller)) throw new ManagementAccessDeniedException();
    }
    private boolean superUser(InternalCallContext caller) { return authorization.findRoleCodes(caller.userId()).contains("R_SUPER"); }
    private void protectAdministrator(Long userId, InternalCallContext caller, boolean removing) {
        Long role = jdbc.queryForObject("SELECT id FROM sys_role WHERE role_code='R_SUPER' FOR UPDATE", Long.class);
        if (count("SELECT COUNT(*) FROM sys_user_role WHERE user_id=? AND role_id=?", userId, role) == 0) return;
        if (!superUser(caller)) throw new ManagementAccessDeniedException();
        if (removing && count("SELECT COUNT(*) FROM sys_user WHERE id=? AND status='ENABLED'", userId) > 0
                && count("SELECT COUNT(*) FROM sys_user_role ur JOIN sys_user u ON u.id=ur.user_id WHERE ur.role_id=? AND u.status='ENABLED'", role) <= 1)
            throw new ManagementConflictException("必须保留至少一个启用的超级管理员");
    }
    private String table(String resource) { String table = TABLES.get(resource); if (table == null) throw new IllegalArgumentException("未知管理对象"); return table; }
    private String permission(String resource, String action) { table(resource); return "system:" + resource.substring(0, resource.length() - 1) + ":" + action; }
    private int count(String sql, Object... args) { Integer count = jdbc.queryForObject(sql, Integer.class, args); return count == null ? 0 : count; }
    private void requireExists(String table, Long id) { if (count("SELECT COUNT(*) FROM " + table + " WHERE id=?", id) != 1) throw new ManagementNotFoundException(); }
    private List<String> ids(String sql, Long id) { return jdbc.queryForList(sql, Long.class, id).stream().map(String::valueOf).toList(); }
    private List<Long> validIds(List<Long> ids) { if (ids == null || ids.size() > 1000 || ids.stream().anyMatch(id -> id == null || id <= 0)) throw new IllegalArgumentException("无效授权列表"); return ids.stream().distinct().toList(); }
    private String snake(String value) { return value.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase(Locale.ROOT); }
    private void terminateUserSessions(Long userId) { jdbc.queryForList("SELECT id FROM sys_user_session WHERE user_id=? AND status='ACTIVE'", Long.class, userId).forEach(auth::kick); }
    private void grantSuper(String resource, Long id) {
        List<Long> roles = jdbc.queryForList("SELECT id FROM sys_role WHERE role_code='R_SUPER'", Long.class);
        for (Long role : roles) jdbc.update("INSERT IGNORE INTO " + (resource.equals("menus") ? "sys_role_menu (id,role_id,menu_id)" : "sys_role_button (id,role_id,button_id)") + " VALUES (?,?,?)", IdWorker.getId(), role, id);
    }
    private void insert(String table, Map<String, Object> values) {
        jdbc.update("INSERT INTO " + table + " (" + String.join(",", values.keySet()) + ") VALUES (" + String.join(",", Collections.nCopies(values.size(), "?")) + ")", values.values().toArray());
    }
    private void update(String table, Long id, Map<String, Object> values) {
        List<Object> args = new ArrayList<>(values.values()); args.add(id);
        jdbc.update("UPDATE " + table + " SET " + String.join(",", values.keySet().stream().map(field -> field + "=?").toList()) + " WHERE id=?", args.toArray());
    }
}
