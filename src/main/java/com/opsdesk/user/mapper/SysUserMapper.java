package com.opsdesk.user.mapper;

import com.opsdesk.user.entity.SysUser;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 用户数据访问 Mapper。
 *
 * <p>只负责 sys_user 表读写，不包含注册、登录或权限判断等业务逻辑。</p>
 */
@Mapper
public interface SysUserMapper {

    @Select("""
            SELECT *
            FROM sys_user
            WHERE phone = #{phone}
              AND deleted = 0
            LIMIT 1
            """)
    SysUser findByPhone(@Param("phone") String phone);

    @Select("""
            SELECT *
            FROM sys_user
            WHERE id = #{id}
              AND deleted = 0
            LIMIT 1
            """)
    SysUser findById(@Param("id") Long id);

    @Select("""
            SELECT COUNT(1)
            FROM sys_user
            WHERE phone = #{phone}
              AND deleted = 0
            """)
    int countByPhone(@Param("phone") String phone);

    @Insert("""
            INSERT INTO sys_user (
              id, phone, password_hash, username, nickname, email, gender, avatar_code, avatar_url,
              department_id, status, create_by, update_by, deleted
            )
            VALUES (
              #{id}, #{phone}, #{passwordHash}, #{username}, #{nickname}, #{email}, #{gender}, #{avatarCode}, #{avatarUrl},
              #{departmentId}, #{status}, #{createBy}, #{updateBy}, 0
            )
            """)
    int insert(SysUser user);

    @Update("""
            UPDATE sys_user
            SET password_hash = #{passwordHash},
                update_by = #{operatorId},
                update_time = CURRENT_TIMESTAMP
            WHERE id = #{userId}
              AND deleted = 0
            """)
    int updatePassword(@Param("userId") Long userId,
                       @Param("passwordHash") String passwordHash,
                       @Param("operatorId") Long operatorId);
}
