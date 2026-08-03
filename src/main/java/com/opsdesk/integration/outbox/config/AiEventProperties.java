package com.opsdesk.integration.outbox.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * AI 知识事件发布配置。
 */
@Component
@ConfigurationProperties(prefix = "opsdesk.ai-events")
public class AiEventProperties {

    private boolean enabled;
    private long dispatchDelayMs = 1000L;
    private int batchSize = 50;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public long getDispatchDelayMs() {
        return dispatchDelayMs;
    }

    public void setDispatchDelayMs(long dispatchDelayMs) {
        this.dispatchDelayMs = dispatchDelayMs;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }
}
