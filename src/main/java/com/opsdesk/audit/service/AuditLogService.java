package com.opsdesk.audit.service;

/**
 * 审计日志服务。
 *
 * <p>用于记录注册、登录、退出、改密和权限变更等关键操作。</p>
 */
public interface AuditLogService {

    void record(Long operatorId,
                String operationType,
                String bizType,
                Long bizId,
                String content,
                String requestIp,
                String userAgent);
}
