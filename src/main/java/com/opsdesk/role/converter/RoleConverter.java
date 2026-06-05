package com.opsdesk.role.converter;

import com.opsdesk.role.entity.Role;
import com.opsdesk.role.vo.RoleVO;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 角色对象转换器。
 *
 * <p>负责将数据库实体转换为接口 VO，并在出口处把 Long ID 统一转为字符串。</p>
 */
@Component
public class RoleConverter {

    public RoleVO toVO(Role role, List<Long> permissionIds) {
        RoleVO vo = new RoleVO();
        vo.setId(String.valueOf(role.getId()));
        vo.setCode(role.getCode());
        vo.setName(role.getName());
        vo.setDescription(role.getDescription());
        vo.setBuiltIn(isTrue(role.getBuiltIn()));
        vo.setEnabled(isTrue(role.getEnabled()));
        vo.setPermissionIds(permissionIds == null
                ? List.of()
                : permissionIds.stream().map(String::valueOf).toList());
        vo.setCreatedAt(role.getCreateTime());
        vo.setUpdatedAt(role.getUpdateTime());
        return vo;
    }

    private boolean isTrue(Integer value) {
        return value != null && value == 1;
    }
}
