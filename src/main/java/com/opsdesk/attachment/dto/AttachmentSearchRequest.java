package com.opsdesk.attachment.dto;

/**
 * 附件列表请求。
 *
 * <p>业务详情使用 bizType + bizId；创建页回显临时附件时使用 bizType + tempToken。</p>
 */
public class AttachmentSearchRequest {

    private String bizType;
    private String bizId;
    private String tempToken;

    public String getBizType() {
        return bizType;
    }

    public void setBizType(String bizType) {
        this.bizType = bizType;
    }

    public String getBizId() {
        return bizId;
    }

    public void setBizId(String bizId) {
        this.bizId = bizId;
    }

    public String getTempToken() {
        return tempToken;
    }

    public void setTempToken(String tempToken) {
        this.tempToken = tempToken;
    }
}
