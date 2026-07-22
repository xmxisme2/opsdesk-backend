package com.opsdesk.system.vo;

/** 邮件通知当前配置，供系统管理员维护默认收件邮箱与开关。 */
public record EmailNotificationSettingsVO(boolean enabled, String defaultRecipient) {}
