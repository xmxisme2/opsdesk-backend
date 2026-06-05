package com.opsdesk.role.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 创建角色请求。
 *
 * <p>角色编码创建后不可修改；权限 ID 按接口契约使用字符串传输，服务层负责解析和校验有效权限。</p>
 */
@Getter
@Setter
public class RoleCreateRequest {

    @NotBlank(message = "角色编码不能为空")
    @Size(max = 64, message = "角色编码不能超过64个字符")
    private String code;

    @NotBlank(message = "角色名称不能为空")
    @Size(max = 64, message = "角色名称不能超过64个字符")
    private String name;

    @Size(max = 255, message = "角色说明不能超过255个字符")
    private String description;

    private Boolean enabled = true;
    private List<String> permissionIds;
}
