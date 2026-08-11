package com.opsdesk.system.vo;

/** 系统配置安全展示对象，敏感配置的 value 固定脱敏，不返回 sensitive 标记。 */
public record SystemConfigVO(String key, String value, String group, String description, boolean editable) {
}
