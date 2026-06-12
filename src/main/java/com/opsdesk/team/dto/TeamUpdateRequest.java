package com.opsdesk.team.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 编辑团队基础信息请求。
 *
 * <p>该请求只维护团队名称、说明、处理范围和启停状态，成员关系由独立成员接口维护。</p>
 */
@Getter
@Setter
public class TeamUpdateRequest {

    @NotBlank(message = "团队名称不能为空")
    @Size(max = 128, message = "团队名称不能超过128个字符")
    private String name;

    @Size(max = 255, message = "团队说明不能超过255个字符")
    private String description;

    /** 兼容接口契约的部门范围字段；当前不单独落库，团队归属部门由成员主属部门实时推导。 */
    private List<String> departmentIds;

    @Size(max = 255, message = "处理范围不能超过255个字符")
    private String processingScope;

    private Boolean enabled = true;
}
