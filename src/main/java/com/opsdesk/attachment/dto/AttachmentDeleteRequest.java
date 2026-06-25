package com.opsdesk.attachment.dto;

/**
 * 附件删除请求。
 *
 * <p>reason 为可选审计说明，附件删除只更新逻辑删除标记。</p>
 */
public class AttachmentDeleteRequest {

    private String reason;

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
