package com.opsdesk.ticket.service.impl;

import com.opsdesk.notification.service.NotificationService;
import com.opsdesk.team.mapper.TeamMemberMapper;
import com.opsdesk.ticket.entity.Ticket;
import com.opsdesk.ticket.mapper.TicketMapper;
import com.opsdesk.ticket.service.TicketOverdueScanService;
import com.opsdesk.user.entity.SysUser;
import com.opsdesk.user.mapper.SysUserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * 工单超时扫描服务实现。
 *
 * <p>扫描逻辑只处理已经提交后的进行中工单，依赖数据库原子更新避免多实例或重复扫描产生重复通知。</p>
 */
@Service
public class TicketOverdueScanServiceImpl implements TicketOverdueScanService {

    /** 单次扫描最大工单数：控制定时任务每轮处理量，避免一次扫描过多影响在线接口。 */
    private static final int SCAN_LIMIT = 200;

    /** 超时通知类型：写入 notification.type，前端按该类型展示“超时提醒”。 */
    private static final String NOTIFICATION_TICKET_OVERDUE = "TICKET_OVERDUE";

    /** 超时通知标题：统一站内通知标题，外部请求不可覆盖。 */
    private static final String OVERDUE_TITLE = "工单已超时";

    private final TicketMapper ticketMapper;
    private final TeamMemberMapper teamMemberMapper;
    private final SysUserMapper sysUserMapper;
    private final NotificationService notificationService;

    public TicketOverdueScanServiceImpl(TicketMapper ticketMapper,
                                        TeamMemberMapper teamMemberMapper,
                                        SysUserMapper sysUserMapper,
                                        NotificationService notificationService) {
        this.ticketMapper = ticketMapper;
        this.teamMemberMapper = teamMemberMapper;
        this.sysUserMapper = sysUserMapper;
        this.notificationService = notificationService;
    }

    @Override
    public int scanOverdueTickets() {
        return scanOverdueTickets(LocalDateTime.now());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int scanOverdueTickets(LocalDateTime now) {
        LocalDateTime scanTime = now == null ? LocalDateTime.now() : now;
        List<Ticket> candidates = ticketMapper.findOverdueCandidates(scanTime, SCAN_LIMIT);
        int updatedCount = 0;
        for (Ticket ticket : candidates) {
            if (ticket == null || ticket.getId() == null) {
                continue;
            }
            if (ticketMapper.markOverdue(ticket.getId()) == 0) {
                continue;
            }
            updatedCount++;
            notifyOverdueTicket(ticket);
        }
        return updatedCount;
    }

    /**
     * 超时通知接收人：当前处理人和团队负责人都需要收到提醒，并通过 Set 去重。
     */
    private void notifyOverdueTicket(Ticket ticket) {
        LinkedHashSet<Long> receiverIds = new LinkedHashSet<>();
        if (ticket.getAssigneeId() != null) {
            receiverIds.add(ticket.getAssigneeId());
        }
        if (ticket.getTeamId() != null) {
            List<Long> leaderIds = teamMemberMapper.findLeaderIdsByTeamId(ticket.getTeamId());
            if (leaderIds != null) {
                receiverIds.addAll(leaderIds);
            }
        }
        receiverIds.remove(null);
        String assignee = resolveAssigneeName(ticket.getAssigneeId());
        Map<String, String> variables = Map.of("ticketNo", displayTicketNo(ticket), "assignee", assignee);
        receiverIds.forEach(receiverId -> notificationService.createTicketNotification(
                receiverId,
                NOTIFICATION_TICKET_OVERDUE,
                variables,
                ticket.getId(),
                null
        ));
    }

    private String displayTicketNo(Ticket ticket) {
        if (ticket == null) {
            return "-";
        }
        return StringUtils.hasText(ticket.getTicketNo()) ? ticket.getTicketNo() : String.valueOf(ticket.getId());
    }

    /**
     * 超时模板中的处理人变量必须是可读姓名，处理人已失效时降级为“待分派”，不泄露内部用户 ID。
     */
    private String resolveAssigneeName(Long assigneeId) {
        if (assigneeId == null) {
            return "待分派";
        }
        SysUser assignee = sysUserMapper.findById(assigneeId);
        if (assignee == null) {
            return "待分派";
        }
        return StringUtils.hasText(assignee.getNickname()) ? assignee.getNickname() : assignee.getUsername();
    }
}
