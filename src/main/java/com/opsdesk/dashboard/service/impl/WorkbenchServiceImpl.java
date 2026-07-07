package com.opsdesk.dashboard.service.impl;

import com.opsdesk.common.exception.BusinessException;
import com.opsdesk.common.exception.ErrorCode;
import com.opsdesk.common.security.CurrentUser;
import com.opsdesk.common.util.IdParser;
import com.opsdesk.dashboard.service.WorkbenchService;
import com.opsdesk.dashboard.vo.WorkbenchSummaryVO;
import com.opsdesk.notification.converter.NotificationConverter;
import com.opsdesk.notification.mapper.NotificationMapper;
import com.opsdesk.notification.vo.NotificationVO;
import com.opsdesk.team.mapper.TeamMapper;
import com.opsdesk.ticket.converter.TicketConverter;
import com.opsdesk.ticket.entity.Ticket;
import com.opsdesk.ticket.mapper.TicketCategoryMapper;
import com.opsdesk.ticket.mapper.TicketMapper;
import com.opsdesk.ticket.vo.TicketListItemVO;
import com.opsdesk.user.mapper.SysUserMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 工作台服务实现。
 *
 * <p>工作台只做轻量聚合，不承载状态流转；工单资源范围与列表接口保持一致。</p>
 */
@Service
public class WorkbenchServiceImpl implements WorkbenchService {

    /** 管理员角色编码：管理员可查看全局工作台数据，外部请求不能伪造该角色。 */
    private static final String ROLE_ADMIN = "ADMIN";

    /** 最近工单数量：对应 Figma 工作台表格展示 4 条记录。 */
    private static final int LATEST_TICKET_LIMIT = 4;

    /** 最近通知数量：工作台摘要只展示少量通知，完整列表由通知中心承载。 */
    private static final int LATEST_NOTIFICATION_LIMIT = 5;

    private final TicketMapper ticketMapper;
    private final TicketCategoryMapper ticketCategoryMapper;
    private final SysUserMapper sysUserMapper;
    private final TeamMapper teamMapper;
    private final NotificationMapper notificationMapper;
    private final TicketConverter ticketConverter;
    private final NotificationConverter notificationConverter;

    public WorkbenchServiceImpl(TicketMapper ticketMapper,
                                TicketCategoryMapper ticketCategoryMapper,
                                SysUserMapper sysUserMapper,
                                TeamMapper teamMapper,
                                NotificationMapper notificationMapper,
                                TicketConverter ticketConverter,
                                NotificationConverter notificationConverter) {
        this.ticketMapper = ticketMapper;
        this.ticketCategoryMapper = ticketCategoryMapper;
        this.sysUserMapper = sysUserMapper;
        this.teamMapper = teamMapper;
        this.notificationMapper = notificationMapper;
        this.ticketConverter = ticketConverter;
        this.notificationConverter = notificationConverter;
    }

    @Override
    public WorkbenchSummaryVO summary(String teamId, CurrentUser currentUser) {
        Long currentUserId = requireUserId(currentUser);
        Long parsedTeamId = parseOptionalId(teamId);
        boolean admin = currentUser.getRoles().contains(ROLE_ADMIN);

        long pendingAssignCount = ticketMapper.countWorkbenchByStatus("PENDING_ASSIGN", null, parsedTeamId, currentUserId, admin);
        long pendingProcessCount = ticketMapper.countWorkbenchByStatus("PENDING_PROCESS", null, parsedTeamId, currentUserId, admin);
        long processingCount = ticketMapper.countWorkbenchByStatus("PROCESSING", null, parsedTeamId, currentUserId, admin);
        long overdueCount = ticketMapper.countWorkbenchByStatus(null, 1, parsedTeamId, currentUserId, admin);
        long createdCount = ticketMapper.countWorkbenchCreated(currentUserId, parsedTeamId);
        long assignedCount = ticketMapper.countWorkbenchAssigned(currentUserId, parsedTeamId);
        long watchingCount = ticketMapper.countWorkbenchWatching(currentUserId, parsedTeamId);
        long unreadNotificationCount = notificationMapper.countUnread(currentUserId);

        List<TicketListItemVO> latestTickets = ticketMapper.findWorkbenchLatest(parsedTeamId, currentUserId, admin, LATEST_TICKET_LIMIT)
                .stream()
                .map(this::toTicketListItem)
                .toList();
        List<NotificationVO> latestNotifications = notificationMapper.findLatestByReceiver(currentUserId, LATEST_NOTIFICATION_LIMIT)
                .stream()
                .map(notificationConverter::toVO)
                .toList();

        return new WorkbenchSummaryVO(
                pendingAssignCount + pendingProcessCount + processingCount,
                createdCount,
                assignedCount,
                watchingCount,
                unreadNotificationCount,
                pendingAssignCount,
                pendingProcessCount,
                processingCount,
                overdueCount,
                latestTickets,
                latestNotifications
        );
    }

    private TicketListItemVO toTicketListItem(Ticket ticket) {
        return ticketConverter.toListItem(
                ticket,
                ticket.getCategoryId() == null ? null : ticketCategoryMapper.findById(ticket.getCategoryId()),
                ticket.getCreatorId() == null ? null : sysUserMapper.findById(ticket.getCreatorId()),
                ticket.getAssigneeId() == null ? null : sysUserMapper.findById(ticket.getAssigneeId()),
                ticket.getTeamId() == null ? null : teamMapper.findById(ticket.getTeamId())
        );
    }

    private Long parseOptionalId(String value) {
        return StringUtils.hasText(value) ? IdParser.parseRequired(value, "团队ID") : null;
    }

    private Long requireUserId(CurrentUser currentUser) {
        if (currentUser == null || currentUser.getUserId() == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "用户未登录");
        }
        return currentUser.getUserId();
    }
}
