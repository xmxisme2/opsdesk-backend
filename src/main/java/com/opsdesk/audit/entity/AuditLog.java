package com.opsdesk.audit.entity;

import lombok.Getter;
import lombok.Setter;

/**
 * 审计日志实体。
 *
 * <p>映射 audit_log，记录注册、登录、退出、改密等关键安全操作。</p>
 */
@Getter
@Setter
public class AuditLog {

    private Long id;
    private Long operatorId;
    private String operationType;
    private String bizType;
    private Long bizId;
    private String content;
    private String requestIp;
    private String userAgent;
    private Long createBy;
    private Long updateBy;
}
