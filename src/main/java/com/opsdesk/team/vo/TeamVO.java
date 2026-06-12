package com.opsdesk.team.vo;

import java.util.List;

/**
 * 团队响应对象。
 *
 * <p>团队列表、详情和成员更新后统一返回该对象，ID 字段全部按字符串输出。</p>
 */
public record TeamVO(
        String id,
        String name,
        String description,
        List<String> departmentIds,
        List<String> leaderIds,
        long memberCount,
        String processingScope,
        Boolean enabled,
        String createdAt,
        String updatedAt
) {
}
