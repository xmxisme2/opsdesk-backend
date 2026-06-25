package com.opsdesk.attachment.vo;

/**
 * 文本附件预览返回对象。
 *
 * <p>图片预览直接输出受权限保护的文件流；该对象主要承载 txt/log 的转义文本和截断标记。</p>
 */
public record AttachmentPreviewVO(
        String fileId,
        String fileName,
        String previewType,
        String previewUrl,
        String content,
        Boolean truncated,
        String downloadUrl
) {
}
