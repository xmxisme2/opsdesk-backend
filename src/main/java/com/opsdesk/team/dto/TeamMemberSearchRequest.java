package com.opsdesk.team.dto;

import com.opsdesk.common.pagination.PageQuery;
import lombok.Getter;
import lombok.Setter;
import org.springframework.util.StringUtils;

/**
 * 团队成员列表查询请求。
 *
 * <p>用于组织管理页查看团队成员，支持按用户手机号、用户名和昵称检索。</p>
 */
@Getter
@Setter
public class TeamMemberSearchRequest extends PageQuery {

    private String keyword;

    public String normalizedKeyword() {
        return StringUtils.hasText(keyword) ? keyword.trim() : null;
    }
}
