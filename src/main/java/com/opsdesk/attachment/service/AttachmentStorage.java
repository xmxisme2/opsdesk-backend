package com.opsdesk.attachment.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

/**
 * 附件内容存储接口。
 *
 * <p>业务层只保存该接口返回的相对路径，后续切换 MinIO 或云存储时不影响附件元数据模型。</p>
 */
public interface AttachmentStorage {

    String store(MultipartFile file, String extension, Long attachmentId);

    Resource load(String storagePath);

    void deleteQuietly(String storagePath);
}
