package com.opsdesk.system.entity;

/** 系统配置实体，用于承载 system_config 中可编辑的键值配置。 */
public class SystemConfig {
    private Long id;
    private String configKey;
    private String configValue;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getConfigKey() { return configKey; }
    public void setConfigKey(String configKey) { this.configKey = configKey; }
    public String getConfigValue() { return configValue; }
    public void setConfigValue(String configValue) { this.configValue = configValue; }
}
