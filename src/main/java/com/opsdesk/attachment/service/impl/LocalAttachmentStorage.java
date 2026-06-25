package com.opsdesk.attachment.service.impl;

import com.opsdesk.attachment.service.AttachmentStorage;
import com.opsdesk.common.exception.BusinessException;
import com.opsdesk.common.exception.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

/**
 * 本地附件存储实现。
 *
 * <p>文件按日期目录和附件 ID 命名，数据库仅保存相对路径；所有读取都校验目标仍位于存储根目录内。</p>
 */
@Service
public class LocalAttachmentStorage implements AttachmentStorage {

    /** 本地存储日志记录器：物理清理失败时记录相对路径，不输出服务器绝对目录。 */
    private static final Logger LOGGER = LoggerFactory.getLogger(LocalAttachmentStorage.class);

    private final Path storageRoot;

    @Autowired
    public LocalAttachmentStorage(@Value("${opsdesk.storage.file-root:storage/files}") String storageRoot) {
        this(Path.of(storageRoot));
    }

    public LocalAttachmentStorage(Path storageRoot) {
        this.storageRoot = storageRoot.toAbsolutePath().normalize();
    }

    @Override
    public String store(MultipartFile file, String extension, Long attachmentId) {
        LocalDate today = LocalDate.now();
        Path relativePath = Path.of(
                String.valueOf(today.getYear()),
                String.format("%02d", today.getMonthValue()),
                String.format("%02d", today.getDayOfMonth()),
                attachmentId + "." + extension
        );
        Path targetPath = resolveSafe(relativePath.toString());
        try {
            Files.createDirectories(targetPath.getParent());
            file.transferTo(targetPath);
            return relativePath.toString().replace('\\', '/');
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED, "附件写入本地存储失败");
        }
    }

    @Override
    public Resource load(String storagePath) {
        Path targetPath = resolveSafe(storagePath);
        if (!Files.isRegularFile(targetPath)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "附件文件不存在");
        }
        return new PathResource(targetPath);
    }

    @Override
    public void deleteQuietly(String storagePath) {
        if (storagePath == null || storagePath.isBlank()) {
            return;
        }
        try {
            Files.deleteIfExists(resolveSafe(storagePath));
        } catch (Exception exception) {
            LOGGER.warn("清理附件物理文件失败 storagePath={}", storagePath, exception);
        }
    }

    private Path resolveSafe(String storagePath) {
        if (storagePath == null || storagePath.isBlank()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "附件存储路径不存在");
        }
        Path targetPath = storageRoot.resolve(storagePath).normalize();
        if (!targetPath.startsWith(storageRoot)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "附件存储路径非法");
        }
        return targetPath;
    }
}
