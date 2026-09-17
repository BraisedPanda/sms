package com.xqy.sms.system.provider.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xqy.sms.common.entity.SysUserSession;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SysUserSessionMapper extends BaseMapper<SysUserSession> {
    @Select("SELECT * FROM sys_user_session WHERE id = #{id} FOR UPDATE")
    SysUserSession selectByIdForUpdate(@Param("id") Long id);
}
