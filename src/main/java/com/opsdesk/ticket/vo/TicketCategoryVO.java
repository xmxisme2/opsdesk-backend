package com.opsdesk.ticket.vo;

import java.util.List;

/**
 * 工单分类树返回对象。
 *
 * <p>包含默认团队和默认 SLA，供创建工单时展示分派建议和截止时间预估。</p>
 */
public record TicketCategoryVO(
        String id,
        String parentId,
        String name,
        String defaultTeamId,
        String defaultTeamName,
        Integer defaultSlaHours,
        Integer sort,
        Boolean enabled,
        String createdAt,
        String updatedAt,
        List<TicketCategoryVO> children
) {
}
