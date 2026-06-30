package com.opsdesk.comment.entity;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 工单评论实体。
 *
 * <p>映射 ticket_comment 表，承载公开评论和内部备注；删除使用 deleted 逻辑删除，不物理移除沟通记录。</p>
 */
@Getter
@Setter
public class TicketComment {

    private Long id;
    private Long ticketId;
    private String content;
    private String commentType;
    private Long authorId;
    private String authorName;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Long createBy;
    private Long updateBy;
    private Integer deleted;
}
