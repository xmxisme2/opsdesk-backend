package com.opsdesk.audit.mapper;

import com.opsdesk.audit.entity.AuditLog;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

/**
 * 审计日志 Mapper。
 *
 * <p>只负责 audit_log 持久化，具体记录内容由业务服务组织。</p>
 */
@Mapper
public interface AuditLogMapper {

    @Insert("""
            INSERT INTO audit_log (
              id, operator_id, operation_type, biz_type, biz_id, content, request_ip, user_agent,
              create_by, update_by, deleted
            )
            VALUES (
              #{id}, #{operatorId}, #{operationType}, #{bizType}, #{bizId}, #{content}, #{requestIp}, #{userAgent},
              #{createBy}, #{updateBy}, 0
            )
            """)
    int insert(AuditLog auditLog);
}
