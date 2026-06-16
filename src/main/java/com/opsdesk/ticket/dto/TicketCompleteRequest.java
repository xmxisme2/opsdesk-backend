package com.opsdesk.ticket.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 工单提交完成请求。
 *
 * <p>由当前处理人提交处理说明，附件字段为后续评论/附件模块联动预留。</p>
 */
@Getter
@Setter
public class TicketCompleteRequest {

    private String completeRemark;

    /** 完成附件 ID：当前首版暂不绑定附件，保留契约兼容字段。 */
    private List<String> attachmentIds;
}
