-- Development bootstrap account: Admin / 123456. Change the password after first login.
INSERT INTO sys_user (id, username, password, nickname, user_type, status)
VALUES (1001, 'Admin', '$2a$10$OrVJdplQkNAnLWHiLKPxFeRZe95TFQ/ejKVBP0CVb2YivBwohC1SG', '系统管理员', 'SYSTEM', 'ENABLED')
ON DUPLICATE KEY UPDATE nickname = VALUES(nickname), status = VALUES(status);

INSERT INTO sys_role (id, role_code, role_name, description, role_type, status)
VALUES (1101, 'R_SUPER', '超级管理员', '系统初始化超级管理员角色', 'SYSTEM', 'ENABLED')
ON DUPLICATE KEY UPDATE role_name = VALUES(role_name), status = VALUES(status);

INSERT INTO sys_user_role (id, user_id, role_id)
VALUES (1201, 1001, 1101)
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

INSERT INTO sys_menu (id, parent_id, path, route_name, component, title, icon, sort_no, keep_alive, visible, hide_tab, full_page, iframe_flag, status)
VALUES
    (1301, NULL, '/system', 'System', '/index/index', 'menus.system.title', 'ri:user-3-line', 90, 0, 1, 0, 0, 0, 'ENABLED'),
    (1302, 1301, 'user', 'User', '/system/user', 'menus.system.user', 'ri:user-line', 10, 1, 1, 0, 0, 0, 'ENABLED'),
    (1303, 1301, 'menu', 'Menus', '/system/menu', 'menus.system.menu', 'ri:menu-line', 20, 1, 1, 0, 0, 0, 'ENABLED')
ON DUPLICATE KEY UPDATE title = VALUES(title), component = VALUES(component), status = VALUES(status);

INSERT INTO sys_role_menu (id, role_id, menu_id)
VALUES (1401, 1101, 1301), (1402, 1101, 1302), (1403, 1101, 1303)
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

INSERT INTO sys_button (id, menu_id, button_name, auth_remark, description, sort_no, status)
VALUES
    (1501, 1302, '查询用户', 'user:read', '查询系统用户', 10, 'ENABLED'),
    (1502, 1303, '新增菜单', 'add', '新增系统菜单', 10, 'ENABLED'),
    (1503, 1303, '编辑菜单', 'edit', '编辑系统菜单', 20, 'ENABLED'),
    (1504, 1303, '删除菜单', 'delete', '删除系统菜单', 30, 'ENABLED')
ON DUPLICATE KEY UPDATE button_name = VALUES(button_name), status = VALUES(status);

INSERT INTO sys_role_button (id, role_id, button_id)
VALUES (1601, 1101, 1501), (1602, 1101, 1502), (1603, 1101, 1503), (1604, 1101, 1504)
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);
