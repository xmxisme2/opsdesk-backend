package com.opsdesk.ticket.vo;

/**
 * 工单列表项返回对象。
 *
 * <p>用于工单列表、我的工单和工作台待办列表，保留列表展示所需的轻量字段。</p>
 */
public record TicketListItemVO(
        String id,
        String ticketNo,
        String title,
        String categoryName,
        String priority,
        String status,
        String creatorName,
        String assigneeName,
        String teamName,
        String dueTime,
        Boolean overdue,
        String createdAt,
        String updatedAt
) {
}
