package com.opsdesk.user.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 用户角色关联 Mapper。
 *
 * <p>负责维护 sys_user_role 关系表，默认注册用户会绑定 USER 角色。</p>
 */
@Mapper
public interface UserRoleMapper {

    @Insert("""
            INSERT INTO sys_user_role (
              id, user_id, role_id, create_by, update_by, deleted
            )
            VALUES (
              #{id}, #{userId}, #{roleId}, #{operatorId}, #{operatorId}, 0
            )
            """)
    int insert(@Param("id") Long id,
               @Param("userId") Long userId,
               @Param("roleId") Long roleId,
               @Param("operatorId") Long operatorId);

    @Select("""
            SELECT role_id
            FROM sys_user_role
            WHERE user_id = #{userId}
              AND deleted = 0
            ORDER BY role_id
            """)
    List<Long> findRoleIdsByUserId(@Param("userId") Long userId);

    @Select("""
            SELECT user_id
            FROM sys_user_role
            WHERE role_id = #{roleId}
              AND deleted = 0
            ORDER BY user_id
            """)
    List<Long> findUserIdsByRoleId(@Param("roleId") Long roleId);

    @Select("""
            SELECT COUNT(1)
            FROM sys_user_role
            WHERE role_id = #{roleId}
              AND deleted = 0
            """)
    int countActiveByRoleId(@Param("roleId") Long roleId);

    @Select("""
            SELECT COUNT(1)
            FROM sys_user_role
            WHERE user_id = #{userId}
              AND role_id = #{roleId}
              AND deleted = 0
            """)
    int countActive(@Param("userId") Long userId,
                    @Param("roleId") Long roleId);

    @Update("""
            UPDATE sys_user_role
            SET deleted = 0,
                update_by = #{operatorId},
                update_time = CURRENT_TIMESTAMP
            WHERE user_id = #{userId}
              AND role_id = #{roleId}
              AND deleted = 1
            LIMIT 1
            """)
    int restoreDeleted(@Param("userId") Long userId,
                       @Param("roleId") Long roleId,
                       @Param("operatorId") Long operatorId);

    @Update("""
            <script>
            UPDATE sys_user_role
            SET deleted = 1,
                update_by = #{operatorId},
                update_time = CURRENT_TIMESTAMP
            WHERE user_id = #{userId}
              AND deleted = 0
            <if test="roleIds != null and roleIds.size > 0">
              AND role_id NOT IN
              <foreach collection="roleIds" item="roleId" open="(" separator="," close=")">
                #{roleId}
              </foreach>
            </if>
            </script>
            """)
    int deactivateMissing(@Param("userId") Long userId,
                          @Param("roleIds") List<Long> roleIds,
                          @Param("operatorId") Long operatorId);
}
