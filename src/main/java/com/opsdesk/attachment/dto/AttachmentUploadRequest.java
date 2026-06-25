package com.opsdesk.attachment.dto;

import org.springframework.web.multipart.MultipartFile;

/**
 * 附件上传请求。
 *
 * <p>绑定附件传 bizId；临时附件不传 bizId，改传 tempToken，两种模式不能同时出现。</p>
 */
public class AttachmentUploadRequest {

    private MultipartFile file;
    private String bizType;
    private String bizId;
    private String tempToken;

    public MultipartFile getFile() {
        return file;
    }

    public void setFile(MultipartFile file) {
        this.file = file;
    }

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
