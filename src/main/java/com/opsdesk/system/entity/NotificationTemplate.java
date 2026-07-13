package com.opsdesk.system.entity;

import java.time.LocalDateTime;

/** 通知模板实体，对应站内及后续扩展渠道的标题、正文和启用状态。 */
public class NotificationTemplate {
    private Long id;
    private String type;
    private String channel;
    private String titleTemplate;
    private String contentTemplate;
    private Integer enabled;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Long updateBy;
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public String getType() { return type; } public void setType(String type) { this.type = type; }
    public String getChannel() { return channel; } public void setChannel(String channel) { this.channel = channel; }
    public String getTitleTemplate() { return titleTemplate; } public void setTitleTemplate(String titleTemplate) { this.titleTemplate = titleTemplate; }
    public String getContentTemplate() { return contentTemplate; } public void setContentTemplate(String contentTemplate) { this.contentTemplate = contentTemplate; }
    public Integer getEnabled() { return enabled; } public void setEnabled(Integer enabled) { this.enabled = enabled; }
    public LocalDateTime getCreateTime() { return createTime; } public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; } public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
    public Long getUpdateBy() { return updateBy; } public void setUpdateBy(Long updateBy) { this.updateBy = updateBy; }
}
