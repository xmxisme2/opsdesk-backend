package com.opsdesk.user.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户角色关联 Mapper。
 *
 * <p>负责维护 sys_user_role 关系表，默认注册用户会绑定 USER 角色。</p>
 */
@Mapper
public interface UserRoleMapper {

    int insert(@Param("id") Long id,
               @Param("userId") Long userId,
               @Param("roleId") Long roleId,
               @Param("operatorId") Long operatorId);

    List<Long> findRoleIdsByUserId(@Param("userId") Long userId);

    List<Long> findUserIdsByRoleId(@Param("roleId") Long roleId);

    int countActiveByRoleId(@Param("roleId") Long roleId);

    int countActive(@Param("userId") Long userId,
                    @Param("roleId") Long roleId);

    int restoreDeleted(@Param("userId") Long userId,
                       @Param("roleId") Long roleId,
                       @Param("operatorId") Long operatorId);

    int deactivateMissing(@Param("userId") Long userId,
                          @Param("roleIds") List<Long> roleIds,
                          @Param("operatorId") Long operatorId);
}
