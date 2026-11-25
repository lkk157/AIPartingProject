package com.aiparking.user.mapper;

import com.aiparking.user.entity.Role;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface RoleMapper extends BaseMapper<Role> {

    @Select("SELECT id FROM role WHERE code = #{code}")
    Long selectIdByCode(@Param("code") String code);

    @Insert("INSERT INTO user_role(user_id, role_id) VALUES(#{userId}, #{roleId})")
    int bindUserRole(@Param("userId") Long userId, @Param("roleId") Long roleId);
}
