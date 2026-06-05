package com.opsdesk.role.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 角色权限关联 Mapper。
 *
 * <p>只维护 sys_role_permission 关系表，权限替换时通过逻辑失效和恢复旧关联避免重复插入。</p>
 */
@Mapper
public interface RolePermissionMapper {

    @Select("""
            SELECT COUNT(1)
            FROM sys_role_permission
            WHERE role_id = #{roleId}
              AND permission_id = #{permissionId}
              AND deleted = 0
            """)
    int countActive(@Param("roleId") Long roleId,
                    @Param("permissionId") Long permissionId);

    @Update("""
            UPDATE sys_role_permission
            SET deleted = 0,
                update_by = #{operatorId},
                update_time = CURRENT_TIMESTAMP
            WHERE role_id = #{roleId}
              AND permission_id = #{permissionId}
              AND deleted = 1
            LIMIT 1
            """)
    int restoreDeleted(@Param("roleId") Long roleId,
                       @Param("permissionId") Long permissionId,
                       @Param("operatorId") Long operatorId);

    @Insert("""
            INSERT INTO sys_role_permission (
              id, role_id, permission_id, create_by, update_by, deleted
            )
            VALUES (
              #{id}, #{roleId}, #{permissionId}, #{operatorId}, #{operatorId}, 0
            )
            """)
    int insert(@Param("id") Long id,
               @Param("roleId") Long roleId,
               @Param("permissionId") Long permissionId,
               @Param("operatorId") Long operatorId);

    @Update("""
            <script>
            UPDATE sys_role_permission
            SET deleted = 1,
                update_by = #{operatorId},
                update_time = CURRENT_TIMESTAMP
            WHERE role_id = #{roleId}
              AND deleted = 0
            <if test="permissionIds != null and permissionIds.size > 0">
              AND permission_id NOT IN
              <foreach collection="permissionIds" item="permissionId" open="(" separator="," close=")">
                #{permissionId}
              </foreach>
            </if>
            </script>
            """)
    int deactivateMissing(@Param("roleId") Long roleId,
                          @Param("permissionIds") List<Long> permissionIds,
                          @Param("operatorId") Long operatorId);
}
