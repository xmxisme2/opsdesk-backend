package com.opsdesk.ticket.vo;

import com.opsdesk.attachment.vo.AttachmentVO;

import java.util.List;

/**
 * 工单详情返回对象。
 *
 * <p>ID 字段统一按字符串输出，附件列表来自附件模块，供详情页直接渲染预览和下载入口。</p>
 */
public record TicketVO(
        String id,
        String ticketNo,
        String title,
        String description,
        String categoryId,
        String categoryName,
        String priority,
        String status,
        String creatorId,
        String creatorName,
        String assigneeId,
        String assigneeName,
        String teamId,
        String teamName,
        String dueTime,
        String completedTime,
        String closedTime,
        Boolean overdue,
        List<String> tags,
        Boolean watching,
        List<AttachmentVO> attachments,
        List<String> availableActions,
        String createdAt,
        String updatedAt
) {
}
