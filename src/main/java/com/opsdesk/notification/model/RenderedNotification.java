package com.opsdesk.notification.model;

/** 已完成变量替换的通知内容，写库前不再包含未解析占位符。 */
public record RenderedNotification(String title, String content) {}
