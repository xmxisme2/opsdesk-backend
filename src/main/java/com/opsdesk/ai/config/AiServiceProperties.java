package com.opsdesk.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 独立 AI 服务代理配置。
 *
 * <p>Service JWT 密钥只从环境变量读取；缺失或长度不足时禁止发起内部调用。</p>
 */
@Component
@ConfigurationProperties(prefix = "opsdesk.ai-service")
public class AiServiceProperties {

    private String baseUrl = "http://127.0.0.1:8081";
    private String serviceJwtSecret = "";
    private String issuer = "opsdesk-backend";
    private String audience = "opsdesk-ai-service";
    private String inboundIssuer = "opsdesk-ai-service";
    private String inboundAudience = "opsdesk-backend";
    private long maxClockSkewSeconds = 30L;
    private long tokenTtlSeconds = 60L;
    private int connectTimeoutSeconds = 3;
    private int readTimeoutSeconds = 10;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getServiceJwtSecret() {
        return serviceJwtSecret;
    }

    public void setServiceJwtSecret(String serviceJwtSecret) {
        this.serviceJwtSecret = serviceJwtSecret;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String getAudience() {
        return audience;
    }

    public void setAudience(String audience) {
        this.audience = audience;
    }

    public String getInboundIssuer() {
        return inboundIssuer;
    }

    public void setInboundIssuer(String inboundIssuer) {
        this.inboundIssuer = inboundIssuer;
    }

    public String getInboundAudience() {
        return inboundAudience;
    }

    public void setInboundAudience(String inboundAudience) {
        this.inboundAudience = inboundAudience;
    }

    public long getMaxClockSkewSeconds() {
        return maxClockSkewSeconds;
    }

    public void setMaxClockSkewSeconds(long maxClockSkewSeconds) {
        this.maxClockSkewSeconds = maxClockSkewSeconds;
    }

    public long getTokenTtlSeconds() {
        return tokenTtlSeconds;
    }

    public void setTokenTtlSeconds(long tokenTtlSeconds) {
        this.tokenTtlSeconds = tokenTtlSeconds;
    }

    public int getConnectTimeoutSeconds() {
        return connectTimeoutSeconds;
    }

    public void setConnectTimeoutSeconds(int connectTimeoutSeconds) {
        this.connectTimeoutSeconds = connectTimeoutSeconds;
    }

    public int getReadTimeoutSeconds() {
        return readTimeoutSeconds;
    }

    public void setReadTimeoutSeconds(int readTimeoutSeconds) {
        this.readTimeoutSeconds = readTimeoutSeconds;
    }
}
