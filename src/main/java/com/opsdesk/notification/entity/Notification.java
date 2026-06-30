package com.opsdesk.notification.entity;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 站内通知实体。
 *
 * <p>映射 notification 表，通知必须关联接收人和可选业务对象，支持未读状态和业务详情跳转。</p>
 */
@Getter
@Setter
public class Notification {

    private Long id;
    private Long receiverId;
    private String type;
    private String title;
    private String content;
    private String bizType;
    private Long bizId;
    private Integer readStatus;
    private LocalDateTime readTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Long createBy;
    private Long updateBy;
    private Integer deleted;
}
