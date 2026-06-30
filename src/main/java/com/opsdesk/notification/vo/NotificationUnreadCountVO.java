package com.opsdesk.notification.vo;

/**
 * 未读通知数量响应。
 *
 * <p>用于顶栏红点、工作台摘要和通知中心入口展示。</p>
 */
public record NotificationUnreadCountVO(long count) {
}
