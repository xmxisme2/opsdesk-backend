package com.opsdesk.ticket.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 工单分派请求。
 *
 * <p>teamId 和 assigneeId 至少传一个；MANAGER 分派时必须落在自己所在团队范围内。</p>
 */
@Getter
@Setter
public class TicketAssignRequest {

    private String teamId;
    private String assigneeId;
    private String reason;
}
