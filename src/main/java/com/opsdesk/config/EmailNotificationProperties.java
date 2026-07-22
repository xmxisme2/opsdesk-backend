package com.opsdesk.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 邮件通知运行配置。
 *
 * <p>邮件开关属于部署环境配置，必须通过 application.yml 或环境变量维护，不能由管理页面运行时开启。</p>
 */
@Component
@ConfigurationProperties(prefix = "opsdesk.email-notification")
public class EmailNotificationProperties {

    /** 邮件通知总开关：仅部署配置可控制，避免误开启外部发送。 */
    private boolean enabled;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
