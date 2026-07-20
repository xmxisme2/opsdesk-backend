package com.opsdesk.ticket.vo;

import java.util.List;

/**
 * 工单分类树返回对象。
 *
 * <p>包含默认团队和默认 SLA，既用于分类树展示，也作为管理员新增、编辑后的统一返回结构。</p>
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
