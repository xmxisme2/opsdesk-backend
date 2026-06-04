package com.opsdesk.permission.mapper;

import com.opsdesk.permission.entity.Permission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 权限数据访问 Mapper。
 *
 * <p>登录后按用户角色加载权限编码，供前端按钮和菜单过滤使用。</p>
 */
@Mapper
public interface PermissionMapper {

    @Select("""
            SELECT DISTINCT p.*
            FROM sys_permission p
            INNER JOIN sys_role_permission rp ON rp.permission_id = p.id
            INNER JOIN sys_user_role ur ON ur.role_id = rp.role_id
            WHERE ur.user_id = #{userId}
              AND ur.deleted = 0
              AND rp.deleted = 0
              AND p.enabled = 1
              AND p.deleted = 0
            ORDER BY p.sort, p.id
            """)
    List<Permission> findEnabledByUserId(@Param("userId") Long userId);
}
