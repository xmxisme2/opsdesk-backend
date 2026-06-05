package com.opsdesk.audit.service.impl;

import com.opsdesk.audit.entity.AuditLog;
import com.opsdesk.audit.mapper.AuditLogMapper;
import com.opsdesk.audit.service.AuditLogService;
import com.opsdesk.common.id.SnowflakeIdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 审计日志服务实现。
 *
 * <p>审计记录失败不阻断主流程，但会写入应用日志，避免登录注册因日志表异常全部不可用。</p>
 */
@Service
public class AuditLogServiceImpl implements AuditLogService {

    /** 审计日志服务记录器：审计写库失败时只写应用日志，不阻断主业务流程。 */
    private static final Logger LOGGER = LoggerFactory.getLogger(AuditLogServiceImpl.class);

    private final AuditLogMapper auditLogMapper;
    private final SnowflakeIdGenerator idGenerator;

    public AuditLogServiceImpl(AuditLogMapper auditLogMapper, SnowflakeIdGenerator idGenerator) {
        this.auditLogMapper = auditLogMapper;
        this.idGenerator = idGenerator;
    }

    @Override
    public void record(Long operatorId,
                       String operationType,
                       String bizType,
                       Long bizId,
                       String content,
                       String requestIp,
                       String userAgent) {
        try {
            AuditLog auditLog = new AuditLog();
            auditLog.setId(idGenerator.nextId());
            auditLog.setOperatorId(operatorId);
            auditLog.setOperationType(operationType);
            auditLog.setBizType(bizType);
            auditLog.setBizId(bizId);
            auditLog.setContent(content);
            auditLog.setRequestIp(requestIp);
            auditLog.setUserAgent(userAgent);
            auditLog.setCreateBy(operatorId);
            auditLog.setUpdateBy(operatorId);
            auditLogMapper.insert(auditLog);
        } catch (Exception exception) {
            LOGGER.warn("写入审计日志失败 operationType={}, bizType={}, bizId={}", operationType, bizType, bizId, exception);
        }
    }
}
