package com.opsdesk.ticket.scheduler;

import com.opsdesk.ticket.service.TicketOverdueScanService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 工单超时定时任务。
 *
 * <p>每 5 分钟扫描一次已超过 SLA 截止时间的未终态工单，真实业务处理委托给服务层，便于测试和复用。</p>
 */
@Component
public class TicketOverdueScheduler {

    private static final Logger log = LoggerFactory.getLogger(TicketOverdueScheduler.class);

    /** 超时扫描间隔：需求约定每 5 分钟扫描一次，单位为毫秒。 */
    private static final long OVERDUE_SCAN_FIXED_DELAY_MS = 5 * 60 * 1000L;

    private final TicketOverdueScanService ticketOverdueScanService;

    public TicketOverdueScheduler(TicketOverdueScanService ticketOverdueScanService) {
        this.ticketOverdueScanService = ticketOverdueScanService;
    }

    @Scheduled(fixedDelay = OVERDUE_SCAN_FIXED_DELAY_MS, initialDelay = OVERDUE_SCAN_FIXED_DELAY_MS)
    public void scanOverdueTickets() {
        int updatedCount = ticketOverdueScanService.scanOverdueTickets();
        if (updatedCount > 0) {
            log.info("Ticket overdue scan marked {} ticket(s)", updatedCount);
        }
    }
}
