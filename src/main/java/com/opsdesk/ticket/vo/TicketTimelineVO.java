package com.opsdesk.ticket.vo;

import java.util.List;

/** 工单混合时间线响应，条目按发生时间倒序返回。 */
public record TicketTimelineVO(List<TicketTimelineItemVO> items) {
}
