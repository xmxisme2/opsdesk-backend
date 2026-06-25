package com.opsdesk.attachment.controller;

import com.opsdesk.attachment.dto.AttachmentUploadRequest;
import com.opsdesk.attachment.entity.Attachment;
import com.opsdesk.attachment.model.AttachmentDownload;
import com.opsdesk.attachment.model.AttachmentPreviewResult;
import com.opsdesk.attachment.service.AttachmentService;
import com.opsdesk.attachment.vo.AttachmentPreviewVO;
import com.opsdesk.attachment.vo.AttachmentVO;
import com.opsdesk.common.response.ApiResponse;
import com.opsdesk.common.security.CurrentUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * 附件 Controller 测试。
 *
 * <p>固定上传统一响应、下载附件响应头以及图片/文本两类预览响应形态。</p>
 */
@ExtendWith(MockitoExtension.class)
class AttachmentControllerTest {

    @Mock
    private AttachmentService attachmentService;

    private AttachmentController controller;
    private CurrentUser currentUser;

    @BeforeEach
    void setUp() {
        controller = new AttachmentController(attachmentService);
        currentUser = new CurrentUser(10L, "13800000000", "user10", List.of("USER"), List.of());
    }

    @Test
    void uploadShouldReturnUnifiedResponse() {
        AttachmentUploadRequest request = new AttachmentUploadRequest();
        AttachmentVO attachmentVO = new AttachmentVO(
                "500", "TICKET", "100", null, "note.txt", 5L, "text/plain", "txt",
                true, "TEXT", false, "/api/files/500/download", "10", "张三", "2026-06-18 10:00:00"
        );
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        when(attachmentService.upload(request, currentUser, "127.0.0.1", null)).thenReturn(attachmentVO);

        ApiResponse<AttachmentVO> response = controller.upload(request, currentUser, servletRequest);

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData()).isEqualTo(attachmentVO);
    }

    @Test
    void downloadShouldReturnAttachmentDisposition() {
        Attachment attachment = attachment("report.pdf", "application/pdf");
        ByteArrayResource resource = new ByteArrayResource("%PDF".getBytes(StandardCharsets.UTF_8));
        when(attachmentService.download("500", currentUser))
                .thenReturn(new AttachmentDownload(attachment, resource));

        var response = controller.download("500", currentUser);

        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PDF);
        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION)).startsWith("attachment;");
        assertThat(response.getBody()).isSameAs(resource);
    }

    @Test
    void previewShouldReturnUnifiedImagePayload() {
        ByteArrayResource resource = new ByteArrayResource(new byte[]{1, 2, 3});
        when(attachmentService.preview("500", currentUser)).thenReturn(new AttachmentPreviewResult(
                "500", "screen.png", "IMAGE", "image/png", 3L, resource,
                null, false, "/api/files/500/download"
        ));

        var response = controller.preview("500", currentUser);

        assertThat(response.getBody()).isInstanceOf(ApiResponse.class);
        ApiResponse<?> body = (ApiResponse<?>) response.getBody();
        assertThat(body.getCode()).isEqualTo(200);
        assertThat(body.getData()).isInstanceOf(AttachmentPreviewVO.class);
        AttachmentPreviewVO preview = (AttachmentPreviewVO) body.getData();
        assertThat(preview.previewUrl()).isEqualTo("/api/files/500/preview/content");
        assertThat(preview.downloadUrl()).isEqualTo("/api/files/500/download");
    }

    @Test
    void previewContentShouldReturnInlineImageResource() {
        ByteArrayResource resource = new ByteArrayResource(new byte[]{1, 2, 3});
        when(attachmentService.preview("500", currentUser)).thenReturn(new AttachmentPreviewResult(
                "500", "screen.png", "IMAGE", "image/png", 3L, resource,
                null, false, "/api/files/500/download"
        ));

        var response = controller.previewContent("500", currentUser);

        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.IMAGE_PNG);
        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION)).startsWith("inline;");
        assertThat(response.getBody()).isSameAs(resource);
    }

    @Test
    void previewShouldReturnUnifiedTextPayload() {
        when(attachmentService.preview("500", currentUser)).thenReturn(new AttachmentPreviewResult(
                "500", "note.txt", "TEXT", "text/plain", 5L, null,
                "hello", false, "/api/files/500/download"
        ));

        var response = controller.preview("500", currentUser);

        assertThat(response.getBody()).isInstanceOf(ApiResponse.class);
        ApiResponse<?> body = (ApiResponse<?>) response.getBody();
        assertThat(body.getCode()).isEqualTo(200);
    }

    private Attachment attachment(String fileName, String contentType) {
        Attachment attachment = new Attachment();
        attachment.setId(500L);
        attachment.setFileName(fileName);
        attachment.setContentType(contentType);
        attachment.setFileSize(4L);
        return attachment;
    }
}
