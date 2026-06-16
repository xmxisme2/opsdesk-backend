package com.opsdesk.ticket.entity;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 工单主表实体。
 *
 * <p>映射 ticket 表，承载工单标题、描述、分类、处理团队、处理人和主状态等核心字段。</p>
 */
@Getter
@Setter
public class Ticket {

    private Long id;
    private String ticketNo;
    private String title;
    private String description;
    private Long categoryId;
    private String priority;
    private String status;
    private Long creatorId;
    private Long assigneeId;
    private Long teamId;
    private LocalDateTime dueTime;
    private LocalDateTime completedTime;
    private LocalDateTime closedTime;
    private Integer overdue;
    private String tags;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Long createBy;
    private Long updateBy;
    private Integer deleted;
}
