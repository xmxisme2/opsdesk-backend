package com.opsdesk.user.mapper;

import com.opsdesk.user.entity.SysUser;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 用户数据访问 Mapper。
 *
 * <p>只负责 sys_user 表读写，不包含注册、登录或权限判断等业务逻辑。</p>
 */
@Mapper
public interface SysUserMapper {

    SysUser findByPhone(@Param("phone") String phone);

    SysUser findByUsername(@Param("username") String username);

    SysUser findById(@Param("id") Long id);

    int countByPhone(@Param("phone") String phone);

    int countByUsername(@Param("username") String username);

    int countByPhoneExcludeId(@Param("phone") String phone,
                              @Param("excludeId") Long excludeId);

    int countByUsernameExcludeId(@Param("username") String username,
                                 @Param("excludeId") Long excludeId);

    int countByDepartmentId(@Param("departmentId") Long departmentId);

    List<SysUser> search(@Param("keyword") String keyword,
                         @Param("departmentId") Long departmentId,
                         @Param("roleCode") String roleCode,
                         @Param("status") String status);

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
