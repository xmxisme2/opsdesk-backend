package com.opsdesk.ticket.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 创建工单请求。
 *
 * <p>用于保存草稿或通过 submitNow 一步提交；工单编号由提交动作生成，草稿阶段不允许前端传入。</p>
 */
@Getter
@Setter
public class TicketCreateRequest {

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

    /** 临时附件 ID：工单创建成功后由服务层在同一事务中完成绑定。 */
    private List<String> attachmentIds;

    private Boolean submitNow = false;
}
