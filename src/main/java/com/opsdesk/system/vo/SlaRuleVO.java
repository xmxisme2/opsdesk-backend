package com.opsdesk.system.vo;

import java.time.LocalDateTime;

/** SLA 规则响应对象，ID 统一按字符串返回。 */
public record SlaRuleVO(
        String id,
        String categoryId,
        String priority,
        Integer responseHours,
        Integer resolveHours,
        Boolean enabled,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
