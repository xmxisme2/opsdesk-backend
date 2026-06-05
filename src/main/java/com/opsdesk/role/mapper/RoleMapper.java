package com.opsdesk.role.mapper;

import com.opsdesk.role.entity.Role;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

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
            WHERE id = #{id}
              AND deleted = 0
            LIMIT 1
            """)
    Role findById(@Param("id") Long id);

    @Select("""
            SELECT *
            FROM sys_role
            WHERE code = #{code}
              AND deleted = 0
            LIMIT 1
            """)
    Role findByCode(@Param("code") String code);

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

    @Select("""
            <script>
            SELECT *
            FROM sys_role
            WHERE deleted = 0
              AND enabled = 1
              AND id IN
              <foreach collection="ids" item="id" open="(" separator="," close=")">
                #{id}
              </foreach>
            ORDER BY id
            </script>
            """)
    List<Role> findEnabledByIds(@Param("ids") List<Long> ids);

    @Select("""
            <script>
            SELECT COUNT(1)
            FROM sys_role
            WHERE deleted = 0
            <if test="keyword != null and keyword != ''">
              AND (code LIKE CONCAT('%', #{keyword}, '%')
                   OR name LIKE CONCAT('%', #{keyword}, '%')
                   OR description LIKE CONCAT('%', #{keyword}, '%'))
            </if>
            <if test="enabled != null">
              AND enabled = #{enabled}
            </if>
            </script>
            """)
    long countSearch(@Param("keyword") String keyword,
                     @Param("enabled") Integer enabled);

    @Select("""
            <script>
            SELECT *
            FROM sys_role
            WHERE deleted = 0
            <if test="keyword != null and keyword != ''">
              AND (code LIKE CONCAT('%', #{keyword}, '%')
                   OR name LIKE CONCAT('%', #{keyword}, '%')
                   OR description LIKE CONCAT('%', #{keyword}, '%'))
            </if>
            <if test="enabled != null">
              AND enabled = #{enabled}
            </if>
            ORDER BY built_in DESC, id ASC
            LIMIT #{size} OFFSET #{offset}
            </script>
            """)
    List<Role> search(@Param("keyword") String keyword,
                      @Param("enabled") Integer enabled,
                      @Param("offset") long offset,
                      @Param("size") long size);

    @Insert("""
            INSERT INTO sys_role (
              id, code, name, description, built_in, enabled, create_by, update_by, deleted
            )
            VALUES (
              #{id}, #{code}, #{name}, #{description}, #{builtIn}, #{enabled}, #{createBy}, #{updateBy}, 0
            )
            """)
    int insert(Role role);

    @Update("""
            UPDATE sys_role
            SET name = #{name},
                description = #{description},
                enabled = #{enabled},
                update_by = #{updateBy},
                update_time = CURRENT_TIMESTAMP
            WHERE id = #{id}
              AND deleted = 0
            """)
    int update(Role role);

    @Update("""
            UPDATE sys_role
            SET deleted = 1,
                update_by = #{operatorId},
                update_time = CURRENT_TIMESTAMP
            WHERE id = #{id}
              AND built_in = 0
              AND deleted = 0
            """)
    int logicalDelete(@Param("id") Long id,
                      @Param("operatorId") Long operatorId);
}
