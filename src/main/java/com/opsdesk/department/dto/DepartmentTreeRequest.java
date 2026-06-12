package com.opsdesk.department.dto;

import lombok.Getter;
import lombok.Setter;
import org.springframework.util.StringUtils;

/**
 * 部门树查询请求。
 *
 * <p>用于注册页、用户管理页和组织管理页加载部门树，支持按关键字和启停状态筛选。</p>
 */
@Getter
@Setter
public class DepartmentTreeRequest {

    private String keyword;
    private Boolean enabled;

    public String normalizedKeyword() {
        return StringUtils.hasText(keyword) ? keyword.trim() : null;
    }
}
