package com.opsdesk.audit.service;

import com.opsdesk.audit.dto.AuditLogSearchRequest;
import com.opsdesk.audit.vo.AuditLogVO;
import com.opsdesk.common.response.PageResult;

/**
 * 审计日志服务。
 *
 * <p>用于记录注册、登录、退出、改密和权限变更等关键操作。</p>
 */
public interface AuditLogService {

    /** 管理员分页检索系统审计日志。 */
    PageResult<AuditLogVO> search(AuditLogSearchRequest request);

    void record(Long operatorId,
                String operationType,
                String bizType,
                Long bizId,
                String content,
                String requestIp,
                String userAgent);
}
