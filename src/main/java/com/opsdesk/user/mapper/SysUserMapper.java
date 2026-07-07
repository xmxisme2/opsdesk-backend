package com.opsdesk.user.mapper;

import com.opsdesk.user.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

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

    int insert(SysUser user);

    int updateProfile(SysUser user);

    int updateStatus(@Param("userId") Long userId,
                     @Param("status") String status,
                     @Param("operatorId") Long operatorId);

    int logicalDelete(@Param("userId") Long userId,
                      @Param("status") String status,
                      @Param("operatorId") Long operatorId);

    int updatePassword(@Param("userId") Long userId,
                       @Param("passwordHash") String passwordHash,
                       @Param("operatorId") Long operatorId);
}
