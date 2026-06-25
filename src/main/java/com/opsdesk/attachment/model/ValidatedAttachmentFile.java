package com.opsdesk.attachment.model;

/**
 * 已通过安全校验的附件文件信息。
 *
 * <p>文件策略校验通过后返回该对象，后续存储和元数据写库只能使用这里的规范化值。</p>
 */
public record ValidatedAttachmentFile(
        String fileName,
        long fileSize,
        String contentType,
        String extension,
        boolean previewable,
        String previewType,
        boolean downloadOnly
) {
}
