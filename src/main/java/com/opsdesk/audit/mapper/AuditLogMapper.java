package com.opsdesk.audit.mapper;

import com.opsdesk.audit.entity.AuditLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 审计日志 Mapper。
 *
 * <p>只负责 audit_log 持久化，具体记录内容由业务服务组织。</p>
 */
@Mapper
public interface AuditLogMapper {

    int insert(AuditLog auditLog);
}
