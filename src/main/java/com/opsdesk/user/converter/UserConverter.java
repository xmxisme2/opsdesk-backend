package com.opsdesk.user.converter;

import com.opsdesk.department.entity.Department;
import com.opsdesk.permission.entity.Permission;
import com.opsdesk.role.entity.Role;
import com.opsdesk.user.entity.SysUser;
import com.opsdesk.user.vo.RoleBriefVO;
import com.opsdesk.user.vo.UserVO;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 用户对象转换器。
 *
 * <p>负责 Entity 到 VO 的字段映射，尤其是数据库 Long ID 到接口字符串 ID 的转换。</p>
 */
@Component
public class UserConverter {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public UserVO toVO(SysUser user,
                       Department department,
                       List<Role> roles,
                       List<Permission> permissions) {
        return new UserVO(
                String.valueOf(user.getId()),
                user.getUsername(),
                user.getNickname(),
                user.getEmail(),
                user.getPhone(),
                user.getGender(),
                user.getAvatarCode(),
                user.getAvatarUrl(),
                user.getDepartmentId() == null ? null : String.valueOf(user.getDepartmentId()),
                department == null ? null : department.getName(),
                roles.stream().map(this::toRoleBriefVO).toList(),
                permissions.stream().map(Permission::getCode).toList(),
                user.getStatus(),
                user.getCreateTime() == null ? null : DATE_TIME_FORMATTER.format(user.getCreateTime()),
                user.getUpdateTime() == null ? null : DATE_TIME_FORMATTER.format(user.getUpdateTime())
        );
    }

    private RoleBriefVO toRoleBriefVO(Role role) {
        return new RoleBriefVO(String.valueOf(role.getId()), role.getCode(), role.getName());
    }
}
