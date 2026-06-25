package com.opsdesk.attachment.model;

import lombok.Getter;
import lombok.Setter;

/**
 * 附件关联业务资源的最小权限投影。
 *
 * <p>评论投影使用 ticketId、ownerId、status，知识库投影使用 ownerId、status，避免附件模块加载无关业务字段。</p>
 */
@Getter
@Setter
public class AttachmentResourceInfo {

    private Long ticketId;
    private Long ownerId;
    private String status;
}
