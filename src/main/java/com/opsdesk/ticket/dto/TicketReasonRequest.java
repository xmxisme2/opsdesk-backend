package com.opsdesk.ticket.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 工单状态动作通用原因请求。
 *
 * <p>用于驳回、重开、关闭、取消和确认完成等只需要补充说明的动作。</p>
 */
@Getter
@Setter
public class TicketReasonRequest {

    private String reason;
    private String comment;
}
