package com.opsdesk.attachment.model;

import org.springframework.core.io.Resource;

/**
 * 附件预览结果。
 *
 * <p>图片使用 resource 输出受保护文件流；文本使用 content 返回转义内容，两者不会同时存在。</p>
 */
public record AttachmentPreviewResult(
        String fileId,
        String fileName,
        String previewType,
        String contentType,
        Long fileSize,
        Resource resource,
        String content,
        Boolean truncated,
        String downloadUrl
) {
}
