package com.xqy.sms.system.provider.mapper;

import com.xqy.sms.common.entity.SysMenu;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface SysAuthorizationMapper {
    @Select("SELECT r.role_code FROM sys_role r INNER JOIN sys_user_role ur ON ur.role_id=r.id WHERE ur.user_id=#{userId} AND r.status='ENABLED'")
    List<String> findRoleCodes(@Param("userId") Long userId);
    @Select("SELECT DISTINCT b.auth_remark FROM sys_button b INNER JOIN sys_role_button rb ON rb.button_id=b.id INNER JOIN sys_user_role ur ON ur.role_id=rb.role_id INNER JOIN sys_role r ON r.id=ur.role_id WHERE ur.user_id=#{userId} AND b.status='ENABLED' AND r.status='ENABLED'")
    List<String> findButtonAuthorities(@Param("userId") Long userId);
    @Select("SELECT DISTINCT m.* FROM sys_menu m INNER JOIN sys_role_menu rm ON rm.menu_id=m.id INNER JOIN sys_user_role ur ON ur.role_id=rm.role_id INNER JOIN sys_role r ON r.id=ur.role_id WHERE ur.user_id=#{userId} AND m.status='ENABLED' AND r.status='ENABLED' ORDER BY m.sort_no,m.id")
    List<SysMenu> findMenus(@Param("userId") Long userId);
    @Select("SELECT DISTINCT b.button_name FROM sys_button b INNER JOIN sys_role_button rb ON rb.button_id=b.id INNER JOIN sys_user_role ur ON ur.role_id=rb.role_id WHERE ur.user_id=#{userId} AND b.menu_id=#{menuId} AND b.status='ENABLED' ORDER BY b.id")
    List<String> findButtonNames(@Param("userId") Long userId, @Param("menuId") Long menuId);
    @Select("SELECT DISTINCT b.auth_remark FROM sys_button b INNER JOIN sys_role_button rb ON rb.button_id=b.id INNER JOIN sys_user_role ur ON ur.role_id=rb.role_id WHERE ur.user_id=#{userId} AND b.menu_id=#{menuId} AND b.status='ENABLED' ORDER BY b.id")
    List<String> findButtonMarks(@Param("userId") Long userId, @Param("menuId") Long menuId);
}
