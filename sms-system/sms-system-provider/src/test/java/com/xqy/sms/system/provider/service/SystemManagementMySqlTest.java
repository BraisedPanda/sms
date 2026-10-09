package com.xqy.sms.system.provider.service;

import com.baomidou.mybatisplus.spring.MybatisSqlSessionFactoryBean;
import com.xqy.sms.common.exception.*;
import com.xqy.sms.common.security.rpc.*;
import com.xqy.sms.system.provider.mapper.SysAuthorizationMapper;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.*;
import javax.sql.DataSource;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@EnabledIfEnvironmentVariable(named="SMS_TEST_MYSQL_URL",matches="jdbc:mysql:.*")
@SpringJUnitConfig(SystemManagementMySqlTest.Config.class)
@Transactional
class SystemManagementMySqlTest {
    static final String SECRET = "01234567890123456789012345678901";
    @Autowired SystemManagementServiceImpl service;
    @Autowired JdbcTemplate jdbc;
    @BeforeEach void fixtures() {
        jdbc.update("INSERT INTO sys_user (id,tenant_id,username,password,user_type,status) VALUES (9001,'default','codex-user','unused','NORMAL','ENABLED'),(9002,'other','codex-other','unused','NORMAL','ENABLED'),(9003,'default','codex-super','unused','NORMAL','ENABLED')");
        jdbc.update("INSERT INTO sys_role(id,role_code,role_name,role_type,status) VALUES(9101,'CODEX_OPERATOR','operator','CUSTOM','ENABLED')");
        jdbc.update("INSERT INTO sys_user_role(id,user_id,role_id) VALUES(9201,9001,9101),(9203,9003,1101)");
        session(9301,1001,"default"); session(9302,9001,"default"); session(9303,9002,"other"); session(9304,9001,"default"); session(9305,9003,"default");
    }
    void session(long id,long user,String tenant) {
        jdbc.update("INSERT INTO sys_user_session(id,tenant_id,user_id,access_token,refresh_token,device_type,login_time,expire_time,refresh_expire_time,status) VALUES(?,?,?,REPEAT('a',64),REPEAT('b',64),'PC',NOW(),DATE_ADD(NOW(),INTERVAL 1 DAY),DATE_ADD(NOW(),INTERVAL 2 DAY),'ACTIVE')",id,tenant,user);
    }
    InternalCallContext admin() { return caller(1001,9301,"default"); }
    InternalCallContext operator() { return caller(9001,9302,"default"); }
    InternalCallContext caller(long user,long session,String tenant) { return new InternalCallSigner(SECRET).sign("sms-web-bff",tenant,user,session,UUID.randomUUID().toString()); }
    void permit(String authority) {
        Long id=jdbc.queryForObject("SELECT id FROM sys_button WHERE auth_remark=? LIMIT 1",Long.class,authority);
        jdbc.update("INSERT INTO sys_role_button(id,role_id,button_id) VALUES(?,9101,?)",com.baomidou.mybatisplus.core.toolkit.IdWorker.getId(),id);
    }
    @Test void createsHashesAndUpdatesOnlyWhitelistedTenantUserFields() {
        var row=service.save("users",null,Map.of("username","codex-created","password","secure123","status","ENABLED"),admin());
        long id=Long.parseLong(row.get("id").toString());
        assertEquals("default",jdbc.queryForObject("SELECT tenant_id FROM sys_user WHERE id=?",String.class,id));
        assertTrue(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().matches("secure123",jdbc.queryForObject("SELECT password FROM sys_user WHERE id=?",String.class,id)));
        assertFalse(row.containsKey("password"));
        assertThrows(IllegalArgumentException.class,()->service.save("users",id,Map.of("tenantId","other"),admin()));
        assertThrows(ManagementNotFoundException.class,()->service.save("users",9002L,Map.of("nickname","x"),admin()));
    }
    @Test void deniesUnprivilegedWritesAndPermissionRemovalImmediately() {
        assertThrows(ManagementAccessDeniedException.class,()->service.list("users",Map.of(),1,20,operator()));
        permit("system:user:read");
        assertTrue(service.list("users",Map.of(),1,20,operator()).getRecords().stream().noneMatch(r->"9002".equals(r.get("id"))));
        jdbc.update("DELETE FROM sys_role_button WHERE role_id=9101");
        assertThrows(ManagementAccessDeniedException.class,()->service.list("users",Map.of(),1,20,operator()));
    }
    @Test void devicesAreScopedAndNeverExposeTokenMaterial() {
        var rows=service.list("sessions",Map.of(),1,20,operator()).getRecords();
        assertEquals(2,rows.size());
        rows.forEach(row->assertTrue(row.keySet().stream().noneMatch(k->k.toLowerCase().contains("token")||k.toLowerCase().contains("jti"))));
        assertThrows(ManagementAccessDeniedException.class,()->service.revokeSession(9301L,operator()));
        assertThrows(ManagementNotFoundException.class,()->service.revokeSession(9303L,admin()));
        service.revokeSession(9304L,operator());
        assertEquals("LOGGED_OUT",jdbc.queryForObject("SELECT status FROM sys_user_session WHERE id=9304",String.class));
    }
    @Test void protectsSuperUsersFromDelegatedPasswordOrRoleChanges() {
        permit("system:user:update"); permit("system:user:grant"); permit("system:user:delete");
        assertThrows(ManagementAccessDeniedException.class,()->service.save("users",9003L,Map.of("password","secure123"),operator()));
        assertThrows(ManagementAccessDeniedException.class,()->service.assignUserRoles(9003L,List.of(1101L),operator()));
        assertThrows(ManagementAccessDeniedException.class,()->service.delete("users",9003L,operator()));
        assertThrows(ManagementConflictException.class,()->service.assignUserRoles(1001L,List.of(),admin()));
    }
    @Test void enforcesParentMenuAndButtonGrantsAndAutoGrantsSuper() {
        var root=service.save("menus",null,Map.of("title","codex-root","status","ENABLED"),admin());
        long rootId=Long.parseLong(root.get("id").toString());
        var child=service.save("menus",null,Map.of("title","codex-child","parentId",rootId,"status","ENABLED"),admin());
        long childId=Long.parseLong(child.get("id").toString());
        var button=service.save("buttons",null,Map.of("menuId",childId,"buttonName","Test","authRemark","codex:test","status","ENABLED"),admin());
        long buttonId=Long.parseLong(button.get("id").toString());
        assertThrows(IllegalArgumentException.class,()->service.save("menus",rootId,Map.of("parentId",childId),admin()));
        assertThrows(IllegalArgumentException.class,()->service.grantRole(9101L,List.of(childId),List.of(buttonId),admin()));
        service.grantRole(9101L,List.of(rootId,childId),List.of(buttonId),admin());
        service.authorize("codex:test",operator());
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM sys_role_button WHERE role_id=1101 AND button_id=?",Integer.class,buttonId));
        assertThrows(ManagementConflictException.class,()->service.delete("menus",rootId,admin()));
    }
    @Configuration @EnableTransactionManagement(proxyTargetClass=true)
    @MapperScan(basePackageClasses=SysAuthorizationMapper.class)
    static class Config {
        @Bean DataSource dataSource() {
            String url=System.getenv("SMS_TEST_MYSQL_URL");
            if(!url.matches("jdbc:mysql://[^/]+/sms_codex_verify_[a-z0-9_]+(?:\\?.*)?")) throw new IllegalArgumentException("Disposable verification schema required");
            return new DriverManagerDataSource(url,System.getenv("SMS_TEST_MYSQL_USERNAME"),System.getenv("SMS_TEST_MYSQL_PASSWORD"));
        }
        @Bean JdbcTemplate jdbc(DataSource source) { return new JdbcTemplate(source); }
        @Bean DataSourceTransactionManager transactionManager(DataSource source) { return new DataSourceTransactionManager(source); }
        @Bean SqlSessionFactory sqlSessionFactory(DataSource source) throws Exception {
            var factory=new MybatisSqlSessionFactoryBean(); factory.setDataSource(source); return factory.getObject();
        }
        @Bean SystemAuthServiceImpl auth(JdbcTemplate jdbc) {
            var auth=mock(SystemAuthServiceImpl.class);
            doAnswer(call->{jdbc.update("UPDATE sys_user_session SET status='LOGGED_OUT' WHERE id=?",call.getArgument(0,Long.class)); return null;}).when(auth).kick(anyLong());
            return auth;
        }
        @Bean SystemManagementServiceImpl management(JdbcTemplate jdbc,SysAuthorizationMapper mapper,SystemAuthServiceImpl auth) {
            return new SystemManagementServiceImpl(jdbc,mapper,auth,SECRET);
        }
    }
}
