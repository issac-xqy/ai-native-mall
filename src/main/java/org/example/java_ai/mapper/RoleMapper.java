package org.example.java_ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.example.java_ai.entity.Role;

import java.util.List;

@Mapper
public interface RoleMapper extends BaseMapper<Role> {

    @Select("""
        SELECT DISTINCT p.perm_code FROM sys_user_role ur
        JOIN sys_role r ON ur.role_id = r.id AND r.status = 1 AND r.deleted = 0
        JOIN sys_role_permission rp ON r.id = rp.role_id AND rp.deleted = 0
        JOIN sys_permission p ON rp.permission_id = p.id AND p.status = 1 AND p.deleted = 0
        WHERE ur.user_id = #{userId} AND ur.deleted = 0
        """)
    List<String> findPermissionsByUserId(Long userId);

    @Select("""
        SELECT DISTINCT r.role_code FROM sys_user_role ur
        JOIN sys_role r ON ur.role_id = r.id AND r.status = 1 AND r.deleted = 0
        WHERE ur.user_id = #{userId} AND ur.deleted = 0
        """)
    List<String> findRoleCodesByUserId(Long userId);
}
