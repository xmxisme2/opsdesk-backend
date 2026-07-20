package com.opsdesk.notification.service;

import com.opsdesk.notification.model.RenderedNotification;

import java.util.Map;
import java.util.Optional;

/** 站内通知模板渲染服务；模板停用时返回空，调用方不创建通知。 */
public interface NotificationTemplateRenderService {
    Optional<RenderedNotification> render(String type, Map<String, String> variables);
}
