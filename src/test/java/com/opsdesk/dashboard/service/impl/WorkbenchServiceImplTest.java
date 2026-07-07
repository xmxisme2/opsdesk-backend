package com.opsdesk.dashboard.service.impl;

import com.opsdesk.common.exception.BusinessException;
import com.opsdesk.common.exception.ErrorCode;
import com.opsdesk.common.security.CurrentUser;
import com.opsdesk.dashboard.vo.WorkbenchSummaryVO;
import com.opsdesk.notification.converter.NotificationConverter;
import com.opsdesk.notification.entity.Notification;
import com.opsdesk.notification.mapper.NotificationMapper;
import com.opsdesk.team.mapper.TeamMapper;
import com.opsdesk.ticket.converter.TicketConverter;
import com.opsdesk.ticket.entity.Ticket;
import com.opsdesk.ticket.mapper.TicketCategoryMapper;
import com.opsdesk.ticket.mapper.TicketMapper;
import com.opsdesk.ticket.vo.TicketListItemVO;
import com.opsdesk.user.mapper.SysUserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 工作台摘要服务测试。
 *
 * <p>覆盖当前用户数据范围、指标聚合和最近通知，保证首页不会越权展示其他人的工单。</p>
 */
@ExtendWith(MockitoExtension.class)
class WorkbenchServiceImplTest {

    @Mock
    private TicketMapper ticketMapper;

    @Mock
    private TicketCategoryMapper ticketCategoryMapper;

    @Mock
    private SysUserMapper sysUserMapper;

    @Mock
    private TeamMapper teamMapper;

    @Mock
    private NotificationMapper notificationMapper;

    private WorkbenchServiceImpl workbenchService;

    @BeforeEach
    void setUp() {
        workbenchService = new WorkbenchServiceImpl(
                ticketMapper,
                ticketCategoryMapper,
                sysUserMapper,
                teamMapper,
                notificationMapper,
                new TicketConverter(),
                new NotificationConverter()
        );
    }

    @Test
    void summaryShouldAggregateCurrentUserWorkbenchData() {
        CurrentUser currentUser = user(10L, "USER");
        when(ticketMapper.countWorkbenchByStatus("PENDING_ASSIGN", null, null, 10L, false)).thenReturn(2L);
        when(ticketMapper.countWorkbenchByStatus("PENDING_PROCESS", null, null, 10L, false)).thenReturn(3L);
        when(ticketMapper.countWorkbenchByStatus("PROCESSING", null, null, 10L, false)).thenReturn(4L);
        when(ticketMapper.countWorkbenchByStatus(null, 1, null, 10L, false)).thenReturn(1L);
        when(ticketMapper.countWorkbenchCreated(10L, null)).thenReturn(5L);
        when(ticketMapper.countWorkbenchAssigned(10L, null)).thenReturn(6L);
        when(ticketMapper.countWorkbenchWatching(10L, null)).thenReturn(7L);
        when(ticketMapper.findWorkbenchLatest(null, 10L, false, 4)).thenReturn(List.of(ticket()));
        when(notificationMapper.countUnread(10L)).thenReturn(8L);
        when(notificationMapper.findLatestByReceiver(10L, 5)).thenReturn(List.of(notification()));

        WorkbenchSummaryVO summary = workbenchService.summary(null, currentUser);

        assertThat(summary.pendingAssignCount()).isEqualTo(2);
        assertThat(summary.pendingProcessCount()).isEqualTo(3);
        assertThat(summary.processingCount()).isEqualTo(4);
        assertThat(summary.overdueCount()).isEqualTo(1);
        assertThat(summary.todoCount()).isEqualTo(9);
        assertThat(summary.createdCount()).isEqualTo(5);
        assertThat(summary.assignedCount()).isEqualTo(6);
        assertThat(summary.watchingCount()).isEqualTo(7);
        assertThat(summary.unreadNotificationCount()).isEqualTo(8);
        assertThat(summary.latestTickets()).extracting(TicketListItemVO::ticketNo).containsExactly("TK202607070001");
        assertThat(summary.latestNotifications()).hasSize(1);
        verify(ticketMapper).findWorkbenchLatest(null, 10L, false, 4);
    }

    @Test
    void summaryShouldRejectAnonymousUser() {
        assertThatThrownBy(() -> workbenchService.summary(null, null))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.UNAUTHORIZED);
    }

    private Ticket ticket() {
        Ticket ticket = new Ticket();
        ticket.setId(100L);
        ticket.setTicketNo("TK202607070001");
        ticket.setTitle("VPN 无法连接");
        ticket.setPriority("URGENT");
        ticket.setStatus("PENDING_ASSIGN");
        ticket.setCreatorId(10L);
        ticket.setOverdue(0);
        ticket.setCreateTime(LocalDateTime.of(2026, 7, 7, 9, 0));
        ticket.setUpdateTime(ticket.getCreateTime());
        return ticket;
    }

    private Notification notification() {
        Notification notification = new Notification();
        notification.setId(200L);
        notification.setReceiverId(10L);
        notification.setType("TICKET_ASSIGNED");
        notification.setTitle("工单已分派");
        notification.setContent("工单已分派给你。");
        notification.setBizType("TICKET");
        notification.setBizId(100L);
        notification.setReadStatus(0);
        notification.setCreateTime(LocalDateTime.of(2026, 7, 7, 9, 10));
        return notification;
    }

    private CurrentUser user(Long userId, String role) {
        return new CurrentUser(userId, "13800000000", "user" + userId, List.of(role), List.of());
    }
}
