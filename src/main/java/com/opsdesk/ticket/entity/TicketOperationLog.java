package com.opsdesk.ticket.entity;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 工单操作日志实体。
 *
 * <p>映射 ticket_operation_log 表，记录工单创建、提交、分派、状态流转等关键操作。</p>
 */
@Getter
@Setter
public class TicketOperationLog {

    private Long id;
    private Long ticketId;
    private String operationType;
    private String fromStatus;
    private String toStatus;
    private Long operatorId;
    private String content;
    private String requestIp;
    private String userAgent;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Long createBy;
    private Long updateBy;
    private Integer deleted;
}
