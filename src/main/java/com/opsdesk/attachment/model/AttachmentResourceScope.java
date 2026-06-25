package com.opsdesk.attachment.model;

/**
 * 已通过权限校验的附件业务范围。
 *
 * <p>ticketId 用于统计单工单附件数量；知识库附件不关联工单时该字段为空。</p>
 */
public record AttachmentResourceScope(
        String bizType,
        Long bizId,
        Long ticketId
) {
}
