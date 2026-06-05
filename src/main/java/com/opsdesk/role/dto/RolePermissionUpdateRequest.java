package com.opsdesk.role.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 角色权限替换请求。
 *
 * <p>权限保存采用整体替换模型，服务层会逻辑失效旧关联、恢复或新增目标关联，并清理受影响用户缓存。</p>
 */
@Getter
@Setter
public class RolePermissionUpdateRequest {

    @NotNull(message = "权限列表不能为空")
    private List<String> permissionIds;
}
