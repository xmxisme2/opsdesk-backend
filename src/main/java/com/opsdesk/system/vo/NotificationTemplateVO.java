package com.opsdesk.system.vo;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** 通知模板响应，额外返回当前类型允许使用的变量供管理端提示。 */
public record NotificationTemplateVO(String id, String type, String channel, String titleTemplate,
                                     String contentTemplate, boolean enabled, List<String> allowedVariables,
                                     Map<String, String> variableDescriptions,
                                     LocalDateTime updatedAt) {}
