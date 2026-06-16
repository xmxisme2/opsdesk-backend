package com.opsdesk.ticket.entity;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 工单分类实体。
 *
 * <p>映射 ticket_category 表，提供创建工单时的分类树、默认处理团队和默认 SLA 小时数。</p>
 */
@Getter
@Setter
public class TicketCategory {

    private Long id;
    private Long parentId;
    private String name;
    private Long defaultTeamId;
    private Integer defaultSlaHours;
    private Integer sort;
    private Integer enabled;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Long createBy;
    private Long updateBy;
    private Integer deleted;
}
