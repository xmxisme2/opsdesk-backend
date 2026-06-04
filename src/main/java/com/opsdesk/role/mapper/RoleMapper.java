package com.opsdesk.role.mapper;

import com.opsdesk.role.entity.Role;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 角色数据访问 Mapper。
 *
 * <p>用于登录上下文加载角色，以及注册时查找默认 USER 角色。</p>
 */
@Mapper
public interface RoleMapper {

    @Select("""
            SELECT *
            FROM sys_role
            WHERE code = #{code}
              AND enabled = 1
              AND deleted = 0
            LIMIT 1
            """)
    Role findEnabledByCode(@Param("code") String code);

    @Select("""
            SELECT r.*
            FROM sys_role r
            INNER JOIN sys_user_role ur ON ur.role_id = r.id
            WHERE ur.user_id = #{userId}
              AND ur.deleted = 0
              AND r.enabled = 1
              AND r.deleted = 0
            ORDER BY r.id
            """)
    List<Role> findEnabledByUserId(@Param("userId") Long userId);
}
