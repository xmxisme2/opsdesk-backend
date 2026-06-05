package com.opsdesk.role.dto;

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
public class RoleSearchRequest {

    private Long page = 1L;
    private Long size = 20L;
    private String keyword;
    private Boolean enabled;

    public long normalizedPage() {
        return page == null || page < 1 ? 1L : page;
    }

    public long normalizedSize() {
        if (size == null || size < 1) {
            return 20L;
        }
        return Math.min(size, 100L);
    }

    public String normalizedKeyword() {
        return StringUtils.hasText(keyword) ? keyword.trim() : null;
    }
}
