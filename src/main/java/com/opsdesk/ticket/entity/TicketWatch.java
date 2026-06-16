package com.opsdesk.ticket.entity;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 工单关注关系实体。
 *
 * <p>映射 ticket_watch 表，用于“我关注的工单”和详情页关注状态。</p>
 */
@Getter
@Setter
public class TicketWatch {

    private Long id;
    private Long ticketId;
    private Long userId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Long createBy;
    private Long updateBy;
    private Integer deleted;
}
