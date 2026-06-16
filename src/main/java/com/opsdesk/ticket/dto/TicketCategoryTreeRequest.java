package com.opsdesk.ticket.dto;

import lombok.Getter;
import lombok.Setter;
import org.springframework.util.StringUtils;

/**
 * 工单分类树查询请求。
 *
 * <p>创建工单和列表筛选共用，enabled 为空时返回全部未删除分类。</p>
 */
@Getter
@Setter
public class TicketCategoryTreeRequest {

    private Boolean enabled;
    private String keyword;

    public String normalizedKeyword() {
        return StringUtils.hasText(keyword) ? keyword.trim() : null;
    }
}
