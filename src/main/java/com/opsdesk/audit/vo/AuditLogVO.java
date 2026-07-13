package com.opsdesk.audit.vo;

import java.time.LocalDateTime;

/**
 * 审计日志响应对象。
 *
 * <p>日志仅供管理员检索，业务 ID 统一转换为字符串返回。</p>
 */
public record AuditLogVO(
        String id,
        String operatorId,
        String operatorName,
        String operationType,
        String bizType,
        String bizId,
        String content,
        String requestIp,
        String userAgent,
        LocalDateTime createdAt
) {
}
