package com.opsdesk.system.dto;

/** 通知模板查询请求，类型和渠道均为可选精确筛选条件。 */
public record NotificationTemplateSearchRequest(String type, String channel) {}
