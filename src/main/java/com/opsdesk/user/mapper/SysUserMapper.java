package com.opsdesk.user.mapper;

import com.opsdesk.user.entity.SysUser;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

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
            WHERE username = #{username}
              AND deleted = 0
            LIMIT 1
            """)
    SysUser findByUsername(@Param("username") String username);

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

    @Select("""
            SELECT COUNT(1)
            FROM sys_user
            WHERE username = #{username}
              AND deleted = 0
            """)
    int countByUsername(@Param("username") String username);

    @Select("""
            SELECT COUNT(1)
            FROM sys_user
            WHERE phone = #{phone}
              AND id <> #{excludeId}
              AND deleted = 0
            """)
    int countByPhoneExcludeId(@Param("phone") String phone,
                              @Param("excludeId") Long excludeId);

    @Select("""
            SELECT COUNT(1)
            FROM sys_user
            WHERE username = #{username}
              AND id <> #{excludeId}
              AND deleted = 0
            """)
    int countByUsernameExcludeId(@Param("username") String username,
                                 @Param("excludeId") Long excludeId);

    @Select("""
            <script>
            SELECT COUNT(DISTINCT u.id)
            FROM sys_user u
            <if test="roleCode != null and roleCode != ''">
              INNER JOIN sys_user_role ur ON ur.user_id = u.id AND ur.deleted = 0
              INNER JOIN sys_role r ON r.id = ur.role_id AND r.deleted = 0 AND r.enabled = 1
            </if>
            WHERE u.deleted = 0
            <if test="keyword != null and keyword != ''">
              AND (u.phone LIKE CONCAT('%', #{keyword}, '%')
                   OR u.username LIKE CONCAT('%', #{keyword}, '%')
                   OR u.nickname LIKE CONCAT('%', #{keyword}, '%')
                   OR u.email LIKE CONCAT('%', #{keyword}, '%'))
            </if>
            <if test="departmentId != null">
              AND u.department_id = #{departmentId}
            </if>
            <if test="roleCode != null and roleCode != ''">
              AND r.code = #{roleCode}
            </if>
            <if test="status != null and status != ''">
              AND u.status = #{status}
            </if>
            </script>
            """)
    long countSearch(@Param("keyword") String keyword,
                     @Param("departmentId") Long departmentId,
                     @Param("roleCode") String roleCode,
                     @Param("status") String status);

    @Select("""
            <script>
            SELECT DISTINCT u.*
            FROM sys_user u
            <if test="roleCode != null and roleCode != ''">
              INNER JOIN sys_user_role ur ON ur.user_id = u.id AND ur.deleted = 0
              INNER JOIN sys_role r ON r.id = ur.role_id AND r.deleted = 0 AND r.enabled = 1
            </if>
            WHERE u.deleted = 0
            <if test="keyword != null and keyword != ''">
              AND (u.phone LIKE CONCAT('%', #{keyword}, '%')
                   OR u.username LIKE CONCAT('%', #{keyword}, '%')
                   OR u.nickname LIKE CONCAT('%', #{keyword}, '%')
                   OR u.email LIKE CONCAT('%', #{keyword}, '%'))
            </if>
            <if test="departmentId != null">
              AND u.department_id = #{departmentId}
            </if>
            <if test="roleCode != null and roleCode != ''">
              AND r.code = #{roleCode}
            </if>
            <if test="status != null and status != ''">
              AND u.status = #{status}
            </if>
            ORDER BY u.create_time DESC, u.id DESC
            LIMIT #{size} OFFSET #{offset}
            </script>
            """)
    List<SysUser> search(@Param("keyword") String keyword,
                         @Param("departmentId") Long departmentId,
                         @Param("roleCode") String roleCode,
                         @Param("status") String status,
                         @Param("offset") long offset,
                         @Param("size") long size);

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
            SET phone = #{phone},
                username = #{username},
                nickname = #{nickname},
                email = #{email},
                gender = #{gender},
                avatar_code = #{avatarCode},
                avatar_url = #{avatarUrl},
                department_id = #{departmentId},
                status = #{status},
                update_by = #{updateBy},
                update_time = CURRENT_TIMESTAMP
            WHERE id = #{id}
              AND deleted = 0
            """)
    int updateProfile(SysUser user);

    @Update("""
            UPDATE sys_user
            SET status = #{status},
                update_by = #{operatorId},
                update_time = CURRENT_TIMESTAMP
            WHERE id = #{userId}
              AND deleted = 0
            """)
    int updateStatus(@Param("userId") Long userId,
                     @Param("status") String status,
                     @Param("operatorId") Long operatorId);

    @Update("""
            UPDATE sys_user
            SET status = #{status},
                deleted = 1,
                update_by = #{operatorId},
                update_time = CURRENT_TIMESTAMP
            WHERE id = #{userId}
              AND deleted = 0
            """)
    int logicalDelete(@Param("userId") Long userId,
                      @Param("status") String status,
                      @Param("operatorId") Long operatorId);

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
