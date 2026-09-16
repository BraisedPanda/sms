package com.xqy.sms.web.infrastructure.persistence.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysAuthorizationMapper {

    @Select("""
            SELECT r.role_code FROM sys_role r
            INNER JOIN sys_user_role ur ON ur.role_id = r.id
            WHERE ur.user_id = #{userId} AND r.status = 'ENABLED'
            """)
    List<String> findRoleCodes(@Param("userId") Long userId);

    @Select("""
            SELECT DISTINCT b.auth_remark FROM sys_button b
            INNER JOIN sys_role_button rb ON rb.button_id = b.id
            INNER JOIN sys_user_role ur ON ur.role_id = rb.role_id
            INNER JOIN sys_role r ON r.id = ur.role_id
            WHERE ur.user_id = #{userId} AND b.status = 'ENABLED' AND r.status = 'ENABLED'
            """)
    List<String> findButtonAuthorities(@Param("userId") Long userId);
}
