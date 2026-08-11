package com.opsdesk.system.dto;

/** 系统配置查询条件；分组按大写编码匹配，关键字匹配配置键和说明。 */
public record SystemConfigSearchRequest(String group, String keyword) {
}
