package com.opsdesk.ticket.vo;

/**
 * 工单操作日志返回对象。
 *
 * <p>用于详情页状态流和操作时间线，记录动作、状态变化、操作者和说明。</p>
 */
public record TicketOperationLogVO(
        String id,
        String ticketId,
        String operationType,
        String fromStatus,
        String toStatus,
        String operatorId,
        String operatorName,
        String content,
        String requestIp,
        String userAgent,
        String createdAt
) {
}
