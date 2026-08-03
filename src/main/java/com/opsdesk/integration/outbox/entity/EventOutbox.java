package com.opsdesk.integration.outbox.entity;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 事务消息 Outbox 实体。
 */
@Getter
@Setter
public class EventOutbox {

    private Long id;
    private String eventId;
    private String aggregateType;
    private Long aggregateId;
    private String eventType;
    private String eventVersion;
    private String routingKey;
    private String payload;
    private String status;
    private Integer retryCount;
    private LocalDateTime nextRetryTime;
    private LocalDateTime publishedTime;
    private String lastError;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Long createBy;
    private Long updateBy;
    private Integer deleted;
}
