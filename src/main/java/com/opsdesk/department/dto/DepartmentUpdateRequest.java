package com.opsdesk.department.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * 编辑部门请求。
 *
 * <p>后台组织管理页保存部门基础信息时使用，服务层负责防止父级循环和同级重名。</p>
 */
@Getter
@Setter
public class DepartmentUpdateRequest {

    private String parentId;

    @NotBlank(message = "部门名称不能为空")
    @Size(max = 128, message = "部门名称不能超过128个字符")
    private String name;

    private String leaderId;
    private Integer sort = 0;
    private Boolean enabled = true;
}
