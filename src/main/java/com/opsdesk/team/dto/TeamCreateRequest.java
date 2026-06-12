package com.opsdesk.team.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 创建团队请求。
 *
 * <p>创建团队时可同时初始化成员和负责人；负责人必须包含在成员集合中。</p>
 */
@Getter
@Setter
public class TeamCreateRequest {

    @NotBlank(message = "团队名称不能为空")
    @Size(max = 128, message = "团队名称不能超过128个字符")
    private String name;

    @Size(max = 255, message = "团队说明不能超过255个字符")
    private String description;

    /** 兼容接口契约的部门范围字段；当前不单独落库，团队归属部门由成员主属部门实时推导。 */
    private List<String> departmentIds;
    private List<String> memberIds;
    private List<String> leaderIds;

    @Size(max = 255, message = "处理范围不能超过255个字符")
    private String processingScope;

    private Boolean enabled = true;
}
