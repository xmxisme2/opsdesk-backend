package com.opsdesk.notification.service;

import com.opsdesk.notification.entity.Notification;

/** 邮件通知发送器；发送失败必须被隔离，不能回滚或影响站内通知。 */
public interface EmailNotificationSender {
    void send(Notification notification);
}
