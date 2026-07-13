package com.opsdesk.attachment.service;

import com.opsdesk.attachment.model.ValidatedAttachmentFile;
import com.opsdesk.common.exception.BusinessException;
import com.opsdesk.common.exception.ErrorCode;
import com.opsdesk.system.service.UploadPolicyService;
import com.opsdesk.system.vo.UploadPolicyVO;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * 附件文件安全策略。
 *
 * <p>集中校验原始文件名、扩展名、MIME 和文件大小，并计算前端所需的预览能力。</p>
 */
@Component
public class AttachmentFilePolicy {

    /** 单文件最大字节数：20MB，来自附件需求，禁止外部请求覆盖。 */
    public static final long DEFAULT_MAX_FILE_SIZE_BYTES = 20L * 1024L * 1024L;

    private final UploadPolicyService uploadPolicyService;

    public AttachmentFilePolicy(UploadPolicyService uploadPolicyService) {
        this.uploadPolicyService = uploadPolicyService;
    }

    /** 图片预览类型：jpg、jpeg、png 使用，允许前端请求受保护的图片流。 */
    public static final String PREVIEW_TYPE_IMAGE = "IMAGE";

    /** 文本预览类型：txt、log 使用，后端返回经过转义且最多 1MB 的文本。 */
    public static final String PREVIEW_TYPE_TEXT = "TEXT";

    /** 仅下载类型：pdf、docx、xlsx、zip 使用，不允许内嵌预览。 */
    public static final String PREVIEW_TYPE_DOWNLOAD_ONLY = "DOWNLOAD_ONLY";

    /** 允许扩展名与声明 MIME 的对应关系：外部文件必须同时命中扩展名和 MIME。 */
    private static final Map<String, Set<String>> ALLOWED_MIME_TYPES = Map.of(
            "jpg", Set.of("image/jpeg"),
            "jpeg", Set.of("image/jpeg"),
            "png", Set.of("image/png"),
            "pdf", Set.of("application/pdf"),
            "docx", Set.of("application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
            "xlsx", Set.of("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
            "txt", Set.of("text/plain"),
            "log", Set.of("text/plain"),
            "zip", Set.of("application/zip", "application/x-zip-compressed")
    );

    /** 可直接图片预览的扩展名：由后端输出受权限保护的图片内容。 */
    private static final Set<String> IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png");

    /** 可直接文本预览的扩展名：由后端读取文本并执行 XSS 转义。 */
    private static final Set<String> TEXT_EXTENSIONS = Set.of("txt", "log");

    /** Office 压缩包最多检查条目数：防止恶意文件用海量目录项拖慢上传校验。 */
    private static final int MAX_OFFICE_ZIP_ENTRIES = 10_000;

    public ValidatedAttachmentFile validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "上传文件不能为空");
        }
        UploadPolicyVO policy = uploadPolicyService.detail();
        if (file.getSize() > policy.maxFileSizeMb() * 1024L * 1024L) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "单个文件不能超过" + policy.maxFileSizeMb() + "MB");
        }

        String fileName = normalizeFileName(file.getOriginalFilename());
        String extension = extractExtension(fileName);
        String contentType = normalizeContentType(file.getContentType());
        Set<String> allowedMimeTypes = ALLOWED_MIME_TYPES.get(extension);
        if (allowedMimeTypes == null || !policy.allowedExtensions().contains(extension)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "不支持的文件扩展名");
        }
        if (!allowedMimeTypes.contains(contentType)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "文件扩展名与MIME类型不匹配");
        }
        validateContentSignature(file, extension);

        String previewType = previewType(extension, policy);
        boolean previewable = !PREVIEW_TYPE_DOWNLOAD_ONLY.equals(previewType);
        return new ValidatedAttachmentFile(
                fileName,
                file.getSize(),
                contentType,
                extension,
                previewable,
                previewType,
                !previewable
        );
    }

    private String normalizeFileName(String originalFileName) {
        if (!StringUtils.hasText(originalFileName)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "文件名不能为空");
        }
        String fileName = originalFileName.trim();
        if (fileName.length() > 255
                || fileName.contains("..")
                || fileName.contains("/")
                || fileName.contains("\\")
                || fileName.contains(":")
                || fileName.chars().anyMatch(Character::isISOControl)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "文件名包含不安全字符");
        }
        return fileName;
    }

    private String extractExtension(String fileName) {
        int separatorIndex = fileName.lastIndexOf('.');
        if (separatorIndex <= 0 || separatorIndex == fileName.length() - 1) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "文件扩展名不能为空");
        }
        return fileName.substring(separatorIndex + 1).toLowerCase(Locale.ROOT);
    }

    private String normalizeContentType(String contentType) {
        if (!StringUtils.hasText(contentType)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "文件MIME类型不能为空");
        }
        int parameterIndex = contentType.indexOf(';');
        String normalized = parameterIndex >= 0 ? contentType.substring(0, parameterIndex) : contentType;
        return normalized.trim().toLowerCase(Locale.ROOT);
    }

    private void validateContentSignature(MultipartFile file, String extension) {
        try (InputStream inputStream = file.getInputStream()) {
            byte[] header = inputStream.readNBytes(8);
            boolean valid = switch (extension) {
                case "jpg", "jpeg" -> startsWith(header, 0xFF, 0xD8, 0xFF);
                case "png" -> startsWith(header, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A);
                case "pdf" -> startsWith(header, 0x25, 0x50, 0x44, 0x46);
                case "docx", "xlsx", "zip" -> startsWith(header, 0x50, 0x4B);
                case "txt", "log" -> true;
                default -> false;
            };
            if (!valid) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "文件内容与扩展名不匹配");
            }
            if ("docx".equals(extension) || "xlsx".equals(extension)) {
                validateOfficePackage(file, extension);
            }
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED, "读取上传文件失败");
        }
    }

    private void validateOfficePackage(MultipartFile file, String extension) throws IOException {
        boolean hasContentTypes = false;
        boolean hasDocumentDirectory = false;
        int entryCount = 0;
        String requiredDirectory = "docx".equals(extension) ? "word/" : "xl/";
        try (ZipInputStream zipInputStream = new ZipInputStream(file.getInputStream())) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                entryCount++;
                if (entryCount > MAX_OFFICE_ZIP_ENTRIES) {
                    throw new BusinessException(ErrorCode.PARAM_ERROR, "Office文件目录项过多");
                }
                String entryName = entry.getName();
                hasContentTypes = hasContentTypes || "[Content_Types].xml".equals(entryName);
                hasDocumentDirectory = hasDocumentDirectory || entryName.startsWith(requiredDirectory);
            }
        }
        if (!hasContentTypes || !hasDocumentDirectory) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "Office文件内容与扩展名不匹配");
        }
    }

    private boolean startsWith(byte[] source, int... expected) {
        if (source.length < expected.length) {
            return false;
        }
        for (int index = 0; index < expected.length; index++) {
            if ((source[index] & 0xFF) != expected[index]) {
                return false;
            }
        }
        return true;
    }

    public int maxFilesPerTicket() {
        return uploadPolicyService.detail().maxFilesPerTicket();
    }

    private String previewType(String extension, UploadPolicyVO policy) {
        if (!policy.previewableExtensions().contains(extension)) {
            return PREVIEW_TYPE_DOWNLOAD_ONLY;
        }
        if (IMAGE_EXTENSIONS.contains(extension)) {
            return PREVIEW_TYPE_IMAGE;
        }
        if (TEXT_EXTENSIONS.contains(extension)) {
            return PREVIEW_TYPE_TEXT;
        }
        return PREVIEW_TYPE_DOWNLOAD_ONLY;
    }
}
