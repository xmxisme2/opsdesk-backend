package com.opsdesk.ticket.service;

import java.time.LocalDateTime;

/**
 * 工单超时扫描服务。
 *
 * <p>由定时任务调用，负责把超过 SLA 截止时间且尚未标记的工单置为超时，并触发站内通知。</p>
 */
public interface TicketOverdueScanService {

    /**
     * 扫描并处理当前时间之前已到期的工单。
     *
     * @return 本次成功标记为超时的工单数量
     */
    int scanOverdueTickets();

    /**
     * 按指定时间扫描超时工单，主要供单元测试和后续批处理复用。
     *
     * @param now 判断超时的业务时间点
     * @return 本次成功标记为超时的工单数量
     */
    int scanOverdueTickets(LocalDateTime now);
}
