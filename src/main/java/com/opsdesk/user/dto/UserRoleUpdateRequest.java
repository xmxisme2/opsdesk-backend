package com.opsdesk.user.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 用户角色绑定请求。
 *
 * <p>用户角色采用整体替换模型，至少保留一个启用角色，避免用户被更新成无角色状态。</p>
 */
@Getter
@Setter
public class UserRoleUpdateRequest {

    @NotEmpty(message = "角色列表不能为空")
    private List<String> roleIds;
}
