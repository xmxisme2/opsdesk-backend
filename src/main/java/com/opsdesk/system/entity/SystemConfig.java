package com.opsdesk.system.entity;

/** 系统配置实体，用于承载 system_config 中可编辑的键值配置及安全展示属性。 */
public class SystemConfig {
    private Long id;
    private String configKey;
    private String configValue;
    private String configGroup;
    private String description;
    private Integer editable;
    private Integer sensitive;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getConfigKey() { return configKey; }
    public void setConfigKey(String configKey) { this.configKey = configKey; }
    public String getConfigValue() { return configValue; }
    public void setConfigValue(String configValue) { this.configValue = configValue; }
    public String getConfigGroup() { return configGroup; }
    public void setConfigGroup(String configGroup) { this.configGroup = configGroup; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Integer getEditable() { return editable; }
    public void setEditable(Integer editable) { this.editable = editable; }
    public Integer getSensitive() { return sensitive; }
    public void setSensitive(Integer sensitive) { this.sensitive = sensitive; }
}
