package com.opsdesk.role.mapper;

import com.opsdesk.role.entity.Role;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

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

    int insert(Role role);

    int update(Role role);

    int logicalDelete(@Param("id") Long id,
                      @Param("operatorId") Long operatorId);
}
