package com.opsdesk.notification.vo;

import java.time.LocalDateTime;

/**
 * 通知响应对象。
 *
 * <p>ID 按字符串返回；read 字段面向前端展示，来源于数据库 read_status。</p>
 */
public record NotificationVO(
        String id,
        String receiverId,
        String type,
        String title,
        String content,
        String bizType,
        String bizId,
        Boolean read,
        LocalDateTime readAt,
        LocalDateTime createdAt
) {
}
