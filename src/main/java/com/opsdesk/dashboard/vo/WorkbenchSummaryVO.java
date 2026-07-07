package com.opsdesk.dashboard.vo;

import com.opsdesk.notification.vo.NotificationVO;
import com.opsdesk.ticket.vo.TicketListItemVO;

import java.util.List;

/**
 * 工作台摘要响应对象。
 *
 * <p>同时保留接口契约中的个人视角计数和 Figma 工作台需要的状态指标。</p>
 */
public record WorkbenchSummaryVO(
        long todoCount,
        long createdCount,
        long assignedCount,
        long watchingCount,
        long unreadNotificationCount,
        long pendingAssignCount,
        long pendingProcessCount,
        long processingCount,
        long overdueCount,
        List<TicketListItemVO> latestTickets,
        List<NotificationVO> latestNotifications
) {
}
