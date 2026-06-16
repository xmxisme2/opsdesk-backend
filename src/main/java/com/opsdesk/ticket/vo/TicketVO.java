package com.opsdesk.ticket.vo;

import java.util.List;

/**
 * 工单详情返回对象。
 *
 * <p>ID 字段统一按字符串输出，附件列表暂留空数组，后续附件模块接入后补齐。</p>
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
        List<Object> attachments,
        String createdAt,
        String updatedAt
) {
}
