package com.opsdesk.ticket.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * 工单转派请求。
 *
 * <p>用于处理过程中调整处理团队或处理人，必须填写原因以便操作日志追踪。</p>
 */
@Getter
@Setter
public class TicketTransferRequest {

    private String targetTeamId;
    private String targetAssigneeId;

    @NotBlank(message = "转派原因不能为空")
    private String reason;
}
