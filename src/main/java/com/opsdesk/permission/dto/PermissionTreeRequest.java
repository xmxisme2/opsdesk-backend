package com.opsdesk.permission.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 权限树查询请求。
 *
 * <p>角色授权页可按权限类型和启停状态筛选，默认返回全部未删除权限。</p>
 */
@Getter
@Setter
public class PermissionTreeRequest {

    private String type;
    private Boolean enabled;
}
