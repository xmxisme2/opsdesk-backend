package com.opsdesk.audit.dto;

import com.opsdesk.common.pagination.PageQuery;
import lombok.Getter;
import lombok.Setter;

/**
 * 审计日志分页检索请求。
 *
 * <p>ID 按接口字符串接收；日期支持 yyyy-MM-dd 或 ISO 日期时间。</p>
 */
@Getter
@Setter
public class AuditLogSearchRequest extends PageQuery {

    private String operatorId;
    private String operationType;
    private String bizType;
    private String bizId;
    private String dateFrom;
    private String dateTo;
    private String keyword;
}
