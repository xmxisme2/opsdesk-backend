package com.opsdesk.ticket.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * 工单分类新增和编辑请求。
 *
 * <p>接口层 ID 统一使用字符串；默认 SLA 可不配置，配置时必须为正整数小时。</p>
 */
@Getter
@Setter
public class TicketCategoryMutationRequest {

    @NotBlank
    private String name;

    private String parentId;

    private String defaultTeamId;

    @Min(1)
    private Integer defaultSlaHours;

    @NotNull
    private Integer sort;

    @NotNull
    private Boolean enabled;
}
