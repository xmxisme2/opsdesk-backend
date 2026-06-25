package com.opsdesk.attachment.vo;

/**
 * 附件对外返回对象。
 *
 * <p>ID 统一按字符串输出，storagePath 永不返回前端；previewable 等字段用于前端决定预览或下载入口。</p>
 */
public record AttachmentVO(
        String id,
        String bizType,
        String bizId,
        String tempToken,
        String fileName,
        Long fileSize,
        String contentType,
        String extension,
        Boolean previewable,
        String previewType,
        Boolean downloadOnly,
        String downloadUrl,
        String uploaderId,
        String uploaderName,
        String createdAt
) {
}
