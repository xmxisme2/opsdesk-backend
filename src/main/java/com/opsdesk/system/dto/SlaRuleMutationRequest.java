package com.opsdesk.system.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/** SLA 规则新增和编辑请求，时限统一按小时保存。 */
@Getter
@Setter
public class SlaRuleMutationRequest {
    @NotBlank(message = "分类不能为空")
    private String categoryId;
    @NotBlank(message = "优先级不能为空")
    private String priority;
    @NotNull(message = "响应时限不能为空")
    @Min(value = 1, message = "响应时限至少为 1 小时")
    @Max(value = 8760, message = "响应时限不能超过 8760 小时")
    private Integer responseHours;
    @NotNull(message = "解决时限不能为空")
    @Min(value = 1, message = "解决时限至少为 1 小时")
    @Max(value = 8760, message = "解决时限不能超过 8760 小时")
    private Integer resolveHours;
    @NotNull(message = "启用状态不能为空")
    private Boolean enabled;
}
