package com.opsdesk.attachment.service.impl;

import com.opsdesk.attachment.converter.AttachmentConverter;
import com.opsdesk.attachment.dto.AttachmentDeleteRequest;
import com.opsdesk.attachment.dto.AttachmentSearchRequest;
import com.opsdesk.attachment.dto.AttachmentUploadRequest;
import com.opsdesk.attachment.entity.Attachment;
import com.opsdesk.attachment.mapper.AttachmentMapper;
import com.opsdesk.attachment.model.AttachmentDownload;
import com.opsdesk.attachment.model.AttachmentPreviewResult;
import com.opsdesk.attachment.model.AttachmentResourceScope;
import com.opsdesk.attachment.model.ValidatedAttachmentFile;
import com.opsdesk.attachment.service.AttachmentFilePolicy;
import com.opsdesk.attachment.service.AttachmentResourceAccessService;
import com.opsdesk.attachment.service.AttachmentService;
import com.opsdesk.attachment.service.AttachmentStorage;
import com.opsdesk.attachment.vo.AttachmentVO;
import com.opsdesk.audit.service.AuditLogService;
import com.opsdesk.common.exception.BusinessException;
import com.opsdesk.common.exception.ErrorCode;
import com.opsdesk.common.id.SnowflakeIdGenerator;
import com.opsdesk.common.security.CurrentUser;
import com.opsdesk.common.util.IdParser;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.util.HtmlUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 附件业务服务实现。
 *
 * <p>集中处理临时/绑定上传、工单数量限制、资源权限、相对路径存储、预览和逻辑删除审计。</p>
 */
@Service
public class AttachmentServiceImpl implements AttachmentService {

    /** 单工单附件上限：包含工单直接附件和所属评论附件，来自附件需求，禁止外部覆盖。 */
    /** 文本预览最大字节数：最多读取前 1MB，超出时返回 truncated=true。 */
    private static final int MAX_TEXT_PREVIEW_BYTES = 1024 * 1024;

    /** 临时附件令牌格式：前端生成 16-64 位字母、数字、下划线或短横线，禁止路径字符。 */
    private static final Pattern TEMP_TOKEN_PATTERN = Pattern.compile("[A-Za-z0-9_-]{16,64}");

    /** 管理员角色编码：允许删除任意有权访问资源的附件，也可管理本人外的临时附件。 */
    private static final String ROLE_ADMIN = "ADMIN";

    /** 附件审计业务类型：audit_log.biz_type 固定写 ATTACHMENT。 */
    private static final String AUDIT_BIZ_TYPE = "ATTACHMENT";

    /** 上传审计动作：附件元数据和文件写入成功后记录。 */
    private static final String AUDIT_OPERATION_UPLOAD = "ATTACHMENT_UPLOAD";

    /** 删除审计动作：附件逻辑删除成功后记录。 */
    private static final String AUDIT_OPERATION_DELETE = "ATTACHMENT_DELETE";

    private final AttachmentMapper attachmentMapper;
    private final AttachmentStorage attachmentStorage;
    private final AttachmentFilePolicy filePolicy;
    private final AttachmentResourceAccessService resourceAccessService;
    private final AttachmentConverter attachmentConverter;
    private final SnowflakeIdGenerator idGenerator;
    private final AuditLogService auditLogService;

    public AttachmentServiceImpl(AttachmentMapper attachmentMapper,
                                 AttachmentStorage attachmentStorage,
                                 AttachmentFilePolicy filePolicy,
                                 AttachmentResourceAccessService resourceAccessService,
                                 AttachmentConverter attachmentConverter,
                                 SnowflakeIdGenerator idGenerator,
                                 AuditLogService auditLogService) {
        this.attachmentMapper = attachmentMapper;
        this.attachmentStorage = attachmentStorage;
        this.filePolicy = filePolicy;
        this.resourceAccessService = resourceAccessService;
        this.attachmentConverter = attachmentConverter;
        this.idGenerator = idGenerator;
        this.auditLogService = auditLogService;
    }

    @Override
    @Transactional
    public AttachmentVO upload(AttachmentUploadRequest request,
                               CurrentUser currentUser,
                               String requestIp,
                               String userAgent) {
        Long operatorId = requireUserId(currentUser);
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "上传请求不能为空");
        }
        String bizType = resourceAccessService.normalizeBizType(request.getBizType());
        Long bizId = parseOptionalId(request.getBizId(), "bizId");
        String tempToken = normalizeBinding(request.getTempToken(), bizId);
        ValidatedAttachmentFile validatedFile = filePolicy.validate(request.getFile());

        AttachmentResourceScope resourceScope = null;
        if (bizId != null) {
            resourceScope = resourceAccessService.requireWriteAccess(bizType, bizId, currentUser);
            enforceTicketAttachmentLimit(resourceScope.ticketId());
        }

        Long attachmentId = idGenerator.nextId();
        String storagePath = attachmentStorage.store(request.getFile(), validatedFile.extension(), attachmentId);
        Attachment attachment = buildAttachment(
                attachmentId,
                bizType,
                bizId,
                tempToken,
                storagePath,
                operatorId,
                currentUser.getUsername(),
                validatedFile
        );
        try {
            if (attachmentMapper.insert(attachment) == 0) {
                throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED, "附件元数据保存失败");
            }
        } catch (RuntimeException exception) {
            attachmentStorage.deleteQuietly(storagePath);
            throw exception;
        }

        auditLogService.record(
                operatorId,
                AUDIT_OPERATION_UPLOAD,
                AUDIT_BIZ_TYPE,
                attachmentId,
                "上传附件：" + attachment.getFileName(),
                requestIp,
                userAgent
        );
        return attachmentConverter.toVO(attachment);
    }

    @Override
    public List<AttachmentVO> search(AttachmentSearchRequest request, CurrentUser currentUser) {
        Long operatorId = requireUserId(currentUser);
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "附件查询请求不能为空");
        }
        String bizType = resourceAccessService.normalizeBizType(request.getBizType());
        Long bizId = parseOptionalId(request.getBizId(), "bizId");
        String tempToken = normalizeBinding(request.getTempToken(), bizId);

        List<Attachment> attachments;
        if (bizId != null) {
            resourceAccessService.requireReadAccess(bizType, bizId, currentUser);
            attachments = attachmentMapper.findByBiz(bizType, bizId);
        } else {
            attachments = attachmentMapper.findByTempToken(bizType, tempToken, operatorId);
        }
        return attachments.stream().map(attachmentConverter::toVO).toList();
    }

    @Override
    public AttachmentDownload download(String id, CurrentUser currentUser) {
        Attachment attachment = requireReadableAttachment(id, currentUser);
        return new AttachmentDownload(attachment, attachmentStorage.load(attachment.getStoragePath()));
    }

    @Override
    public AttachmentPreviewResult preview(String id, CurrentUser currentUser) {
        Attachment attachment = requireReadableAttachment(id, currentUser);
        String downloadUrl = "/api/files/" + attachment.getId() + "/download";
        if (AttachmentFilePolicy.PREVIEW_TYPE_IMAGE.equals(attachment.getPreviewType())) {
            return new AttachmentPreviewResult(
                    String.valueOf(attachment.getId()),
                    attachment.getFileName(),
                    attachment.getPreviewType(),
                    attachment.getContentType(),
                    attachment.getFileSize(),
                    attachmentStorage.load(attachment.getStoragePath()),
                    null,
                    false,
                    downloadUrl
            );
        }
        if (AttachmentFilePolicy.PREVIEW_TYPE_TEXT.equals(attachment.getPreviewType())) {
            return textPreview(attachment, downloadUrl);
        }
        throw new BusinessException(ErrorCode.STATE_CONFLICT, "该附件不支持预览，请直接下载");
    }

    @Override
    @Transactional
    public void delete(String id,
                       AttachmentDeleteRequest request,
                       CurrentUser currentUser,
                       String requestIp,
                       String userAgent) {
        Long operatorId = requireUserId(currentUser);
        Attachment attachment = findRequired(IdParser.parseRequired(id, "附件ID"));
        if (attachment.getBizId() != null) {
            resourceAccessService.requireWriteAccess(attachment.getBizType(), attachment.getBizId(), currentUser);
        } else if (!sameUser(operatorId, attachment.getUploaderId()) && !hasRole(currentUser, ROLE_ADMIN)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "当前用户无权删除该临时附件");
        }
        if (!sameUser(operatorId, attachment.getUploaderId()) && !hasRole(currentUser, ROLE_ADMIN)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅上传者或管理员可删除附件");
        }
        if (attachmentMapper.logicalDelete(attachment.getId(), operatorId) == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "附件不存在");
        }

        String reason = normalizeReason(request == null ? null : request.getReason());
        String content = "删除附件：" + attachment.getFileName();
        if (StringUtils.hasText(reason)) {
            content += "，原因：" + reason;
        }
        auditLogService.record(
                operatorId,
                AUDIT_OPERATION_DELETE,
                AUDIT_BIZ_TYPE,
                attachment.getId(),
                content,
                requestIp,
                userAgent
        );
    }

    private Attachment buildAttachment(Long attachmentId,
                                       String bizType,
                                       Long bizId,
                                       String tempToken,
                                       String storagePath,
                                       Long operatorId,
                                       String uploaderName,
                                       ValidatedAttachmentFile validatedFile) {
        Attachment attachment = new Attachment();
        attachment.setId(attachmentId);
        attachment.setBizType(bizType);
        attachment.setBizId(bizId);
        attachment.setTempToken(tempToken);
        attachment.setFileName(validatedFile.fileName());
        attachment.setFileSize(validatedFile.fileSize());
        attachment.setContentType(validatedFile.contentType());
        attachment.setExtension(validatedFile.extension());
        attachment.setPreviewable(validatedFile.previewable() ? 1 : 0);
        attachment.setPreviewType(validatedFile.previewType());
        attachment.setDownloadOnly(validatedFile.downloadOnly() ? 1 : 0);
        attachment.setStoragePath(storagePath);
        attachment.setUploaderId(operatorId);
        attachment.setUploaderName(uploaderName);
        attachment.setCreateTime(LocalDateTime.now());
        attachment.setUpdateTime(attachment.getCreateTime());
        attachment.setCreateBy(operatorId);
        attachment.setUpdateBy(operatorId);
        attachment.setDeleted(0);
        return attachment;
    }

    private void enforceTicketAttachmentLimit(Long ticketId) {
        if (ticketId == null) {
            return;
        }
        if (attachmentMapper.lockTicket(ticketId) == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "工单不存在");
        }
        int maxFiles = filePolicy.maxFilesPerTicket();
        if (attachmentMapper.countActiveByTicketScope(ticketId) >= maxFiles) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT, "单个工单最多上传" + maxFiles + "个附件");
        }
    }

    private AttachmentPreviewResult textPreview(Attachment attachment, String downloadUrl) {
        Resource resource = attachmentStorage.load(attachment.getStoragePath());
        try (InputStream inputStream = resource.getInputStream()) {
            byte[] bytes = inputStream.readNBytes(MAX_TEXT_PREVIEW_BYTES + 1);
            boolean truncated = bytes.length > MAX_TEXT_PREVIEW_BYTES;
            int contentLength = Math.min(bytes.length, MAX_TEXT_PREVIEW_BYTES);
            String content = new String(bytes, 0, contentLength, StandardCharsets.UTF_8);
            return new AttachmentPreviewResult(
                    String.valueOf(attachment.getId()),
                    attachment.getFileName(),
                    attachment.getPreviewType(),
                    attachment.getContentType(),
                    attachment.getFileSize(),
                    null,
                    HtmlUtils.htmlEscape(content),
                    truncated,
                    downloadUrl
            );
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "读取附件预览内容失败");
        }
    }

    private Attachment requireReadableAttachment(String id, CurrentUser currentUser) {
        Long operatorId = requireUserId(currentUser);
        Attachment attachment = findRequired(IdParser.parseRequired(id, "附件ID"));
        if (attachment.getBizId() != null) {
            resourceAccessService.requireReadAccess(attachment.getBizType(), attachment.getBizId(), currentUser);
        } else if (!sameUser(operatorId, attachment.getUploaderId()) && !hasRole(currentUser, ROLE_ADMIN)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "当前用户无权访问该临时附件");
        }
        return attachment;
    }

    private Attachment findRequired(Long attachmentId) {
        Attachment attachment = attachmentMapper.findById(attachmentId);
        if (attachment == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "附件不存在");
        }
        return attachment;
    }

    private String normalizeBinding(String tempToken, Long bizId) {
        boolean hasTempToken = StringUtils.hasText(tempToken);
        if (bizId != null && hasTempToken) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "bizId和tempToken不能同时传入");
        }
        if (bizId == null && !hasTempToken) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "临时附件必须提供tempToken");
        }
        if (!hasTempToken) {
            return null;
        }
        String normalized = tempToken.trim();
        if (!TEMP_TOKEN_PATTERN.matcher(normalized).matches()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "tempToken格式不正确");
        }
        return normalized;
    }

    private String normalizeReason(String reason) {
        if (!StringUtils.hasText(reason)) {
            return null;
        }
        String normalized = reason.trim();
        if (normalized.length() > 200) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "删除原因不能超过200个字符");
        }
        return normalized;
    }

    private Long parseOptionalId(String value, String fieldName) {
        return StringUtils.hasText(value) ? IdParser.parseRequired(value, fieldName) : null;
    }

    private Long requireUserId(CurrentUser currentUser) {
        if (currentUser == null || currentUser.getUserId() == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "用户未登录");
        }
        return currentUser.getUserId();
    }

    private boolean hasRole(CurrentUser currentUser, String role) {
        return currentUser.getRoles().contains(role);
    }

    private boolean sameUser(Long left, Long right) {
        return left != null && left.equals(right);
    }
}
