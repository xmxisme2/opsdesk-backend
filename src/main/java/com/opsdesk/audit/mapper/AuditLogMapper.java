package com.opsdesk.audit.mapper;

import com.opsdesk.audit.entity.AuditLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 审计日志 Mapper。
 *
 * <p>只负责 audit_log 持久化，具体记录内容由业务服务组织。</p>
 */
@Mapper
public interface AuditLogMapper {

    int insert(AuditLog auditLog);

    /**
     * 按后台筛选条件查询审计日志，分页由 Service 使用 PageHelper 统一处理。
     */
    List<AuditLog> search(@Param("operatorId") Long operatorId,
                          @Param("operationType") String operationType,
                          @Param("bizType") String bizType,
                          @Param("bizId") Long bizId,
                          @Param("dateFrom") LocalDateTime dateFrom,
                          @Param("dateTo") LocalDateTime dateTo,
                          @Param("keyword") String keyword);
}
