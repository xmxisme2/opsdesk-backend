package com.opsdesk.ticket.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 编辑草稿工单请求。
 *
 * <p>仅 DRAFT 状态且创建人本人可编辑，提交后的工单必须通过状态动作流转。</p>
 */
@Getter
@Setter
public class TicketUpdateRequest {

    @NotBlank(message = "工单标题不能为空")
    @Size(max = 200, message = "工单标题不能超过200个字符")
    private String title;

    @NotBlank(message = "问题描述不能为空")
    private String description;

    @NotBlank(message = "工单分类不能为空")
    private String categoryId;

    private String priority = "MEDIUM";
    private String dueTime;
    private List<String> tags;

    /** 附件 ID 集合：为后续附件模块预留，当前只保持 API 兼容。 */
    private List<String> attachmentIds;
}
