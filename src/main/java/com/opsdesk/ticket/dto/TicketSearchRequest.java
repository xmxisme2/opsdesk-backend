package com.opsdesk.ticket.dto;

import com.opsdesk.common.pagination.PageQuery;
import lombok.Getter;
import lombok.Setter;
import org.springframework.util.StringUtils;

/**
 * 工单列表查询请求。
 *
 * <p>分页字段复用统一 PageQuery，资源范围由后端根据当前登录用户自动收敛。</p>
 */
@Getter
@Setter
public class TicketSearchRequest extends PageQuery {

    private String scope;
    private String ticketNo;
    private String keyword;
    private String status;
    private String priority;
    private String categoryId;
    private String creatorId;
    private String assigneeId;
    private String teamId;
    private Boolean overdue;
    private String createdFrom;
    private String createdTo;
    /** 导出格式：当前仅支持 xlsx；列表查询忽略该字段，避免影响现有分页契约。 */
    private String format;

    public String normalizedKeyword() {
        return StringUtils.hasText(keyword) ? keyword.trim() : null;
    }

    public String normalizedTicketNo() {
        return StringUtils.hasText(ticketNo) ? ticketNo.trim() : null;
    }
}
