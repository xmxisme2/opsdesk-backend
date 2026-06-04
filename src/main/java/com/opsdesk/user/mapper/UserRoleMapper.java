package com.opsdesk.user.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

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
}
