package com.opsdesk.permission.converter;

import com.opsdesk.permission.entity.Permission;
import com.opsdesk.permission.vo.PermissionVO;
import org.springframework.stereotype.Component;

/**
 * 权限对象转换器。
 *
 * <p>将权限实体转换为前端树节点，保持菜单、按钮和接口权限的通用字段一致。</p>
 */
@Component
public class PermissionConverter {

    public PermissionVO toVO(Permission permission) {
        PermissionVO vo = new PermissionVO();
        vo.setId(String.valueOf(permission.getId()));
        vo.setCode(permission.getCode());
        vo.setName(permission.getName());
        vo.setType(permission.getType());
        vo.setParentId(permission.getParentId() == null ? null : String.valueOf(permission.getParentId()));
        vo.setPath(permission.getPath());
        vo.setMethod(permission.getMethod());
        vo.setSort(permission.getSort());
        vo.setEnabled(permission.getEnabled() != null && permission.getEnabled() == 1);
        return vo;
    }
}
