package com.opsdesk.ticket.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 工单提交完成请求。
 *
 * <p>由当前处理人提交可沉淀的解决方案；保留 completeRemark 兼容历史客户端。</p>
 */
@Getter
@Setter
public class TicketCompleteRequest {

    private String completeRemark;

    /** 解决方案摘要：描述根因或最终处理结论，生成知识草稿时作为解决方案正文。 */
    private String resolutionSummary;

    /** 处理步骤：使用 Markdown 记录可复用的操作过程，生成知识草稿时单独成节。 */
    private String resolutionSteps;

    /** 是否已验证解决结果：由处理人显式确认，外部可传 true 或 false。 */
    private Boolean resolutionVerified;

    /** 完成附件 ID：当前首版暂不绑定附件，保留契约兼容字段。 */
    private List<String> attachmentIds;
}
