package com.opsdesk.permission.mapper;

import com.opsdesk.permission.entity.Permission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 权限数据访问 Mapper。
 *
 * <p>登录后按用户角色加载权限编码，供前端按钮和菜单过滤使用。</p>
 */
@Mapper
public interface PermissionMapper {

    List<Permission> findEnabledByUserId(@Param("userId") Long userId);

    List<Permission> findAll(@Param("type") String type,
                             @Param("enabled") Integer enabled);

    List<Permission> findEnabledByIds(@Param("ids") List<Long> ids);

    List<Long> findIdsByRoleId(@Param("roleId") Long roleId);
}
