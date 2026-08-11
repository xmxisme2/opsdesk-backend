package com.opsdesk.ticket.vo;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 工单混合时间线条目。
 *
 * <p>统一承载操作日志、评论和附件事件，供详情页按时间顺序展示。</p>
 */
@Getter
@Setter
public class TicketTimelineItemVO {
    private String type;
    private String title;
    private String content;
    private String operatorName;
    private LocalDateTime createdAt;
}
