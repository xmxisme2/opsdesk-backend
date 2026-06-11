package com.opsdesk.role.dto;

import com.opsdesk.common.pagination.PageQuery;
import lombok.Getter;
import lombok.Setter;
import org.springframework.util.StringUtils;

/**
 * 角色列表查询请求。
 *
 * <p>承载角色管理页的分页、关键字和启停筛选参数，分页在服务层按默认值和最大值统一收敛。</p>
 */
@Getter
@Setter
public class RoleSearchRequest extends PageQuery {

    private String keyword;
    private Boolean enabled;

    public String normalizedKeyword() {
        return StringUtils.hasText(keyword) ? keyword.trim() : null;
    }
}
