package com.opsdesk.audit.converter;

import com.opsdesk.audit.entity.AuditLog;
import com.opsdesk.audit.vo.AuditLogVO;
import org.springframework.stereotype.Component;

/** 审计日志实体到接口响应的转换器。 */
@Component
public class AuditLogConverter {

    public AuditLogVO toVO(AuditLog auditLog) {
        if (auditLog == null) {
            return null;
        }
        return new AuditLogVO(
                String.valueOf(auditLog.getId()),
                auditLog.getOperatorId() == null ? null : String.valueOf(auditLog.getOperatorId()),
                auditLog.getOperatorName(),
                auditLog.getOperationType(),
                auditLog.getBizType(),
                auditLog.getBizId() == null ? null : String.valueOf(auditLog.getBizId()),
                auditLog.getContent(),
                auditLog.getRequestIp(),
                auditLog.getUserAgent(),
                auditLog.getCreateTime()
        );
    }
}
