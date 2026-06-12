package com.opsdesk.department.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * 创建部门请求。
 *
 * <p>后台组织管理页新增部门时使用；接口层 ID 继续按字符串传输，由服务层解析为数据库 Long。</p>
 */
@Getter
@Setter
public class DepartmentCreateRequest {

    private String parentId;

    @NotBlank(message = "部门名称不能为空")
    @Size(max = 128, message = "部门名称不能超过128个字符")
    private String name;

    private String leaderId;
    private Integer sort = 0;
    private Boolean enabled = true;
}
