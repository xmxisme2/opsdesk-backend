package com.opsdesk.attachment.model;

import com.opsdesk.attachment.entity.Attachment;
import org.springframework.core.io.Resource;

/**
 * 附件下载结果。
 *
 * <p>Controller 使用元数据构造响应头，Resource 只在权限校验通过后加载。</p>
 */
public record AttachmentDownload(
        Attachment attachment,
        Resource resource
) {
}
