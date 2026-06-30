package com.opsdesk.notification.vo;

/**
 * 全部已读响应。
 *
 * <p>返回本次实际更新的通知数量，供前端提示用户操作结果。</p>
 */
public record NotificationReadAllVO(int updatedCount) {
}
