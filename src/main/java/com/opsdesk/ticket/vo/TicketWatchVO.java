package com.opsdesk.ticket.vo;

/**
 * 工单关注状态返回对象。
 *
 * <p>关注和取消关注接口共用，便于前端直接同步详情页按钮状态。</p>
 */
public record TicketWatchVO(Boolean watching) {
}
