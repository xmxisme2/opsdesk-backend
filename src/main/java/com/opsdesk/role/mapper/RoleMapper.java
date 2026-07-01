package com.opsdesk.role.mapper;

import com.opsdesk.role.entity.Role;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 角色数据访问 Mapper。
 *
 * <p>用于登录上下文加载角色，以及注册时查找默认 USER 角色。</p>
 */
@Mapper
public interface RoleMapper {

    Role findById(@Param("id") Long id);

    Role findByCode(@Param("code") String code);

    Role findEnabledByCode(@Param("code") String code);

    List<Role> findEnabledByUserId(@Param("userId") Long userId);

    List<Role> findEnabledByIds(@Param("ids") List<Long> ids);

    List<Role> search(@Param("keyword") String keyword,
                      @Param("enabled") Integer enabled);

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
