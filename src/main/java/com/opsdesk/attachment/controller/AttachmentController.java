package com.opsdesk.attachment.controller;

import com.opsdesk.attachment.dto.AttachmentDeleteRequest;
import com.opsdesk.attachment.dto.AttachmentSearchRequest;
import com.opsdesk.attachment.dto.AttachmentUploadRequest;
import com.opsdesk.attachment.entity.Attachment;
import com.opsdesk.attachment.model.AttachmentDownload;
import com.opsdesk.attachment.model.AttachmentPreviewResult;
import com.opsdesk.attachment.service.AttachmentService;
import com.opsdesk.attachment.vo.AttachmentPreviewVO;
import com.opsdesk.attachment.vo.AttachmentVO;
import com.opsdesk.common.exception.BusinessException;
import com.opsdesk.common.exception.ErrorCode;
import com.opsdesk.common.idempotency.Idempotent;
import com.opsdesk.common.ratelimit.RateLimit;
import com.opsdesk.common.ratelimit.RateLimitDefaults;
import com.opsdesk.common.ratelimit.RateLimitKeyType;
import com.opsdesk.common.response.ApiResponse;
import com.opsdesk.common.security.CurrentUser;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 附件文件 Controller。
 *
 * <p>只负责 multipart/JSON 参数接收、权限入口、限流幂等、响应头和调用 Service，不直接访问本地文件路径。</p>
 */
@RestController
@RequestMapping("/api/files")
public class AttachmentController {

    /** 上传接口每分钟上限：同登录用户最多 20 次，叠加文件大小和单工单数量校验。 */
    private static final int UPLOAD_LIMIT_PER_MINUTE = 20;

    private final AttachmentService attachmentService;

    public AttachmentController(AttachmentService attachmentService) {
        this.attachmentService = attachmentService;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    @Idempotent
    @RateLimit(limit = UPLOAD_LIMIT_PER_MINUTE,
            windowSeconds = RateLimitDefaults.ONE_MINUTE_SECONDS,
            keyType = RateLimitKeyType.USER)
    public ApiResponse<AttachmentVO> upload(@ModelAttribute AttachmentUploadRequest request,
                                            @AuthenticationPrincipal CurrentUser currentUser,
                                            HttpServletRequest servletRequest) {
        return ApiResponse.success(attachmentService.upload(
                request,
                currentUser,
                requestIp(servletRequest),
                userAgent(servletRequest)
        ));
    }

    @PostMapping("/search")
    @PreAuthorize("isAuthenticated()")
    @RateLimit(limit = RateLimitDefaults.SEARCH_LIMIT_PER_MINUTE,
            windowSeconds = RateLimitDefaults.ONE_MINUTE_SECONDS,
            keyType = RateLimitKeyType.USER)
    public ApiResponse<List<AttachmentVO>> search(@RequestBody AttachmentSearchRequest request,
                                                  @AuthenticationPrincipal CurrentUser currentUser) {
        return ApiResponse.success(attachmentService.search(request, currentUser));
    }

    @RequestMapping(value = "/{id}/download", method = {RequestMethod.GET, RequestMethod.POST})
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Resource> download(@PathVariable String id,
                                             @AuthenticationPrincipal CurrentUser currentUser) {
        AttachmentDownload download = attachmentService.download(id, currentUser);
        Attachment attachment = download.attachment();
        return fileStreamResponse(download.resource(), attachment.getContentType(), attachment.getFileSize(),
                attachment.getFileName(), "attachment");
    }

    @PostMapping("/{id}/preview")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<AttachmentPreviewVO>> preview(@PathVariable String id,
                                                                    @AuthenticationPrincipal CurrentUser currentUser) {
        AttachmentPreviewResult preview = attachmentService.preview(id, currentUser);
        AttachmentPreviewVO previewVO = new AttachmentPreviewVO(
                preview.fileId(),
                preview.fileName(),
                preview.previewType(),
                preview.resource() == null ? null : "/api/files/" + preview.fileId() + "/preview/content",
                preview.content(),
                preview.truncated(),
                preview.downloadUrl()
        );
        return ResponseEntity.ok(ApiResponse.success(previewVO));
    }

    @RequestMapping(value = "/{id}/preview/content", method = {RequestMethod.GET, RequestMethod.POST})
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Resource> previewContent(@PathVariable String id,
                                                   @AuthenticationPrincipal CurrentUser currentUser) {
        AttachmentPreviewResult preview = attachmentService.preview(id, currentUser);
        return fileStreamResponse(preview.resource(), preview.contentType(), preview.fileSize(),
                preview.fileName(), "inline");
    }

    @PostMapping("/{id}/delete")
    @PreAuthorize("isAuthenticated()")
    @Idempotent
    @RateLimit(limit = RateLimitDefaults.ACTION_LIMIT_PER_MINUTE,
            windowSeconds = RateLimitDefaults.ONE_MINUTE_SECONDS,
            keyType = RateLimitKeyType.USER)
    public ApiResponse<Void> delete(@PathVariable String id,
                                    @RequestBody(required = false) AttachmentDeleteRequest request,
                                    @AuthenticationPrincipal CurrentUser currentUser,
                                    HttpServletRequest servletRequest) {
        attachmentService.delete(
                id,
                request,
                currentUser,
                requestIp(servletRequest),
                userAgent(servletRequest)
        );
        return ApiResponse.success();
    }

    private MediaType mediaType(String contentType) {
        try {
            return MediaType.parseMediaType(contentType);
        } catch (Exception exception) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    private String contentDisposition(String type, String fileName) {
        ContentDisposition disposition = "inline".equals(type)
                ? ContentDisposition.inline().filename(fileName, StandardCharsets.UTF_8).build()
                : ContentDisposition.attachment().filename(fileName, StandardCharsets.UTF_8).build();
        return disposition.toString();
    }

    private ResponseEntity<Resource> fileStreamResponse(Resource resource,
                                                        String contentType,
                                                        Long fileSize,
                                                        String fileName,
                                                        String dispositionType) {
        if (resource == null) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT, "该附件没有可输出的预览文件流");
        }
        return ResponseEntity.ok()
                .contentType(mediaType(contentType))
                .contentLength(fileSize == null ? 0L : fileSize)
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition(dispositionType, fileName))
                .body(resource);
    }

    private String requestIp(HttpServletRequest servletRequest) {
        return servletRequest.getRemoteAddr();
    }

    private String userAgent(HttpServletRequest servletRequest) {
        return servletRequest.getHeader("User-Agent");
    }
}
