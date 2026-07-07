package com.opsdesk.role.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 角色权限关联 Mapper。
 *
 * <p>只维护 sys_role_permission 关系表，权限替换时通过逻辑失效和恢复旧关联避免重复插入。</p>
 */
@Mapper
public interface RolePermissionMapper {

    int countActive(@Param("roleId") Long roleId,
                    @Param("permissionId") Long permissionId);

    int restoreDeleted(@Param("roleId") Long roleId,
                       @Param("permissionId") Long permissionId,
                       @Param("operatorId") Long operatorId);

    int insert(@Param("id") Long id,
               @Param("roleId") Long roleId,
               @Param("permissionId") Long permissionId,
               @Param("operatorId") Long operatorId);

    int deactivateMissing(@Param("roleId") Long roleId,
                          @Param("permissionIds") List<Long> permissionIds,
                          @Param("operatorId") Long operatorId);
}
