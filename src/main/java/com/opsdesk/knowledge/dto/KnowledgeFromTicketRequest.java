package com.opsdesk.knowledge.dto;

import lombok.Getter;
import lombok.Setter;

/** 从已完成工单生成知识库草稿的选项。 */
@Getter
@Setter
public class KnowledgeFromTicketRequest {
    private Boolean includeComments = Boolean.TRUE;
    private Boolean includeAttachments = Boolean.TRUE;
}
