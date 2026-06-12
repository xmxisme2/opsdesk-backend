package com.opsdesk.department.vo;

import java.util.List;

/**
 * 部门响应对象。
 *
 * <p>部门树和详情接口统一返回该对象，ID 字段按字符串输出，避免前端大整数精度问题。</p>
 */
public record DepartmentVO(
        String id,
        String parentId,
        String name,
        String leaderId,
        String leaderName,
        Integer sort,
        Boolean enabled,
        long memberCount,
        String createdAt,
        String updatedAt,
        List<DepartmentVO> children
) {
}
