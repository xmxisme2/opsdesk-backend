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

    /**
     * 严格记录关键审计日志，写库失败时直接传播异常，由调用方事务统一回滚。
     *
     * <p>仅用于“业务变更与审计必须同时成功”的关键配置等场景；普通调用继续使用尽力记录的 {@link #record}。</p>
     */
    void recordStrict(Long operatorId,
                      String operationType,
                      String bizType,
                      Long bizId,
                      String content,
                      String requestIp,
                      String userAgent);
}
