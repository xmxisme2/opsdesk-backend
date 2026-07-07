package com.opsdesk.ticket.service.impl;

import com.opsdesk.notification.service.NotificationService;
import com.opsdesk.team.mapper.TeamMemberMapper;
import com.opsdesk.ticket.entity.Ticket;
import com.opsdesk.ticket.mapper.TicketMapper;
import com.opsdesk.ticket.service.TicketOverdueScanService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;

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
    private final NotificationService notificationService;

    public TicketOverdueScanServiceImpl(TicketMapper ticketMapper,
                                        TeamMemberMapper teamMemberMapper,
                                        NotificationService notificationService) {
        this.ticketMapper = ticketMapper;
        this.teamMemberMapper = teamMemberMapper;
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
        String content = "工单 " + displayTicketNo(ticket) + " 已超过 SLA 截止时间，请及时处理";
        receiverIds.forEach(receiverId -> notificationService.createTicketNotification(
                receiverId,
                NOTIFICATION_TICKET_OVERDUE,
                OVERDUE_TITLE,
                content,
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
}
