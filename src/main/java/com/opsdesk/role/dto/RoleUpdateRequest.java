package com.opsdesk.role.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 编辑角色请求。
 *
 * <p>只允许维护名称、说明、启停和权限集合，内置角色编码等关键身份字段不开放修改。</p>
 */
@Getter
@Setter
public class RoleUpdateRequest {

    @NotBlank(message = "角色名称不能为空")
    @Size(max = 64, message = "角色名称不能超过64个字符")
    private String name;

    @Size(max = 255, message = "角色说明不能超过255个字符")
    private String description;

    private Boolean enabled;
    private List<String> permissionIds;
}
