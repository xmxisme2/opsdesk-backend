package com.opsdesk.user.dto;

import com.opsdesk.common.pagination.PageQuery;
import lombok.Getter;
import lombok.Setter;
import org.springframework.util.StringUtils;

/**
 * 用户列表查询请求。
 *
 * <p>用于后台用户管理页按关键字、部门、角色和状态检索用户，分页字段继承统一 PageQuery。</p>
 */
@Getter
@Setter
public class UserSearchRequest extends PageQuery {

    private String keyword;
    private String departmentId;
    private String roleCode;
    private String status;

    public String normalizedKeyword() {
        return StringUtils.hasText(keyword) ? keyword.trim() : null;
    }

    public String normalizedRoleCode() {
        return StringUtils.hasText(roleCode) ? roleCode.trim().toUpperCase() : null;
    }

    public String normalizedStatus() {
        return StringUtils.hasText(status) ? status.trim().toUpperCase() : null;
    }
}
