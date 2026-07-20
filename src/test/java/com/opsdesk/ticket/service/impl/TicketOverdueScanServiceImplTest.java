package com.opsdesk.ticket.service.impl;

import com.opsdesk.notification.service.NotificationService;
import com.opsdesk.team.mapper.TeamMemberMapper;
import com.opsdesk.ticket.entity.Ticket;
import com.opsdesk.ticket.mapper.TicketMapper;
import com.opsdesk.user.entity.SysUser;
import com.opsdesk.user.mapper.SysUserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 工单超时扫描服务测试。
 *
 * <p>覆盖定时任务背后的业务服务，确保超时标记和站内通知只在首次命中时发生，避免重复提醒。</p>
 */
@ExtendWith(MockitoExtension.class)
class TicketOverdueScanServiceImplTest {

    @Mock
    private TicketMapper ticketMapper;

    @Mock
    private TeamMemberMapper teamMemberMapper;

    @Mock
    private SysUserMapper sysUserMapper;

    @Mock
    private NotificationService notificationService;

    private TicketOverdueScanServiceImpl overdueScanService;

    @BeforeEach
    void setUp() {
        overdueScanService = new TicketOverdueScanServiceImpl(ticketMapper, teamMemberMapper, sysUserMapper, notificationService);
    }

    @Test
    void scanShouldMarkOverdueAndNotifyAssigneeAndTeamLeaders() {
        LocalDateTime now = LocalDateTime.of(2026, 7, 3, 10, 0);
        Ticket ticket = overdueTicket(100L, 20L, 1L);
        when(ticketMapper.findOverdueCandidates(now, 200)).thenReturn(List.of(ticket));
        when(ticketMapper.markOverdue(100L)).thenReturn(1);
        when(teamMemberMapper.findLeaderIdsByTeamId(1L)).thenReturn(List.of(20L, 30L));
        when(sysUserMapper.findById(20L)).thenReturn(user(20L, "处理人小王"));

        int updatedCount = overdueScanService.scanOverdueTickets(now);

        assertThat(updatedCount).isEqualTo(1);
        verify(notificationService).createTicketNotification(20L, "TICKET_OVERDUE",
                java.util.Map.of("ticketNo", "TK202607030001", "assignee", "处理人小王"), 100L, null);
        verify(notificationService).createTicketNotification(30L, "TICKET_OVERDUE",
                java.util.Map.of("ticketNo", "TK202607030001", "assignee", "处理人小王"), 100L, null);
    }

    @Test
    void scanShouldNotifyTeamLeadersWhenTicketHasNoAssignee() {
        LocalDateTime now = LocalDateTime.of(2026, 7, 3, 10, 0);
        Ticket ticket = overdueTicket(101L, null, 2L);
        when(ticketMapper.findOverdueCandidates(now, 200)).thenReturn(List.of(ticket));
        when(ticketMapper.markOverdue(101L)).thenReturn(1);
        when(teamMemberMapper.findLeaderIdsByTeamId(2L)).thenReturn(List.of(31L));

        int updatedCount = overdueScanService.scanOverdueTickets(now);

        assertThat(updatedCount).isEqualTo(1);
        verify(notificationService).createTicketNotification(31L, "TICKET_OVERDUE",
                java.util.Map.of("ticketNo", "TK202607030002", "assignee", "待分派"), 101L, null);
    }

    @Test
    void scanShouldSkipNotificationWhenTicketWasAlreadyMarkedByAnotherWorker() {
        LocalDateTime now = LocalDateTime.of(2026, 7, 3, 10, 0);
        Ticket ticket = overdueTicket(100L, 20L, 1L);
        when(ticketMapper.findOverdueCandidates(now, 200)).thenReturn(List.of(ticket));
        when(ticketMapper.markOverdue(100L)).thenReturn(0);

        int updatedCount = overdueScanService.scanOverdueTickets(now);

        assertThat(updatedCount).isZero();
        verify(notificationService, never()).createTicketNotification(
                20L,
                "TICKET_OVERDUE",
                "工单已超时",
                "工单 TK202607030001 已超过 SLA 截止时间，请及时处理",
                100L,
                null
        );
    }

    private Ticket overdueTicket(Long id, Long assigneeId, Long teamId) {
        Ticket ticket = new Ticket();
        ticket.setId(id);
        ticket.setTicketNo(id == 100L ? "TK202607030001" : "TK202607030002");
        ticket.setTitle("无法登录系统");
        ticket.setStatus("PROCESSING");
        ticket.setAssigneeId(assigneeId);
        ticket.setTeamId(teamId);
        ticket.setOverdue(0);
        ticket.setDueTime(LocalDateTime.of(2026, 7, 3, 9, 0));
        return ticket;
    }

    /** 创建超时扫描所需的最小处理人测试数据。 */
    private SysUser user(Long id, String nickname) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setNickname(nickname);
        user.setUsername(nickname);
        return user;
    }
}
