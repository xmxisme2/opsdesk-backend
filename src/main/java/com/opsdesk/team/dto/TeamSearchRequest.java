package com.opsdesk.team.dto;

import com.opsdesk.common.pagination.PageQuery;
import lombok.Getter;
import lombok.Setter;
import org.springframework.util.StringUtils;

/**
 * 团队列表查询请求。
 *
 * <p>用于组织管理页和工单分派选择团队，分页字段复用统一 PageQuery。</p>
 */
@Getter
@Setter
public class TeamSearchRequest extends PageQuery {

    private String keyword;
    private String departmentId;
    private Boolean enabled;

    public String normalizedKeyword() {
        return StringUtils.hasText(keyword) ? keyword.trim() : null;
    }
}
