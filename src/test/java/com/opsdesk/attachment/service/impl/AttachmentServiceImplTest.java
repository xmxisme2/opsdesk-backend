package com.opsdesk.attachment.service.impl;

import com.opsdesk.attachment.converter.AttachmentConverter;
import com.opsdesk.attachment.dto.AttachmentDeleteRequest;
import com.opsdesk.attachment.dto.AttachmentSearchRequest;
import com.opsdesk.attachment.dto.AttachmentUploadRequest;
import com.opsdesk.attachment.entity.Attachment;
import com.opsdesk.attachment.mapper.AttachmentMapper;
import com.opsdesk.attachment.model.AttachmentResourceScope;
import com.opsdesk.attachment.service.AttachmentFilePolicy;
import com.opsdesk.attachment.service.AttachmentResourceAccessService;
import com.opsdesk.attachment.service.AttachmentStorage;
import com.opsdesk.audit.service.AuditLogService;
import com.opsdesk.common.exception.BusinessException;
import com.opsdesk.common.exception.ErrorCode;
import com.opsdesk.common.id.SnowflakeIdGenerator;
import com.opsdesk.common.security.CurrentUser;
import com.opsdesk.system.service.UploadPolicyService;
import com.opsdesk.system.vo.UploadPolicyVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 附件服务单元测试。
 *
 * <p>覆盖上传、数量限制、业务列表、下载预览安全和逻辑删除审计等核心行为。</p>
 */
@ExtendWith(MockitoExtension.class)
class AttachmentServiceImplTest {

    @Mock
    private AttachmentMapper attachmentMapper;

    @Mock
    private AttachmentStorage attachmentStorage;

    @Mock
    private AttachmentResourceAccessService resourceAccessService;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private UploadPolicyService uploadPolicyService;

    private AttachmentServiceImpl attachmentService;

    @BeforeEach
    void setUp() {
        org.mockito.Mockito.lenient().when(uploadPolicyService.detail()).thenReturn(new UploadPolicyVO(20, 10,
                List.of("jpg", "jpeg", "png", "pdf", "docx", "xlsx", "txt", "log", "zip"),
                List.of("jpg", "jpeg", "png", "txt", "log"), List.of("pdf", "docx", "xlsx", "zip")));
        attachmentService = new AttachmentServiceImpl(
                attachmentMapper,
                attachmentStorage,
                new AttachmentFilePolicy(uploadPolicyService),
                resourceAccessService,
                new AttachmentConverter(),
                new SnowflakeIdGenerator(),
                auditLogService
        );
    }

    @Test
    void uploadShouldPersistTemporaryAttachmentWithoutBusinessId() {
        AttachmentUploadRequest request = uploadRequest(null, "temp_1234567890abcdef");
        when(resourceAccessService.normalizeBizType("TICKET")).thenReturn("TICKET");
        when(attachmentStorage.store(any(), eq("txt"), anyLong())).thenReturn("2026/06/18/100.txt");
        when(attachmentMapper.insert(any(Attachment.class))).thenReturn(1);

        var result = attachmentService.upload(request, user(10L, "USER"), "127.0.0.1", "JUnit");

        ArgumentCaptor<Attachment> captor = ArgumentCaptor.forClass(Attachment.class);
        verify(attachmentMapper).insert(captor.capture());
        assertThat(captor.getValue().getBizId()).isNull();
        assertThat(captor.getValue().getTempToken()).isEqualTo("temp_1234567890abcdef");
        assertThat(captor.getValue().getStoragePath()).isEqualTo("2026/06/18/100.txt");
        assertThat(result.bizId()).isNull();
        assertThat(result.tempToken()).isEqualTo("temp_1234567890abcdef");
        assertThat(result.previewType()).isEqualTo("TEXT");
        assertThat(result.uploaderName()).isEqualTo("user10");
        verify(resourceAccessService, never()).requireWriteAccess(any(), any(), any());
    }

    @Test
    void uploadShouldRejectEleventhAttachmentInTicketScope() {
        AttachmentUploadRequest request = uploadRequest("100", null);
        CurrentUser currentUser = user(10L, "USER");
        when(resourceAccessService.normalizeBizType("TICKET")).thenReturn("TICKET");
        when(resourceAccessService.requireWriteAccess("TICKET", 100L, currentUser))
                .thenReturn(new AttachmentResourceScope("TICKET", 100L, 100L));
        when(attachmentMapper.lockTicket(100L)).thenReturn(100L);
        when(attachmentMapper.countActiveByTicketScope(100L)).thenReturn(10L);

        assertThatThrownBy(() -> attachmentService.upload(request, currentUser, "127.0.0.1", "JUnit"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.STATE_CONFLICT);

        verifyNoInteractions(attachmentStorage);
        verify(attachmentMapper, never()).insert(any());
    }

    @Test
    void searchShouldReturnPreviewFlagsWithoutStoragePath() {
        AttachmentSearchRequest request = new AttachmentSearchRequest();
        request.setBizType("TICKET");
        request.setBizId("100");
        CurrentUser currentUser = user(10L, "USER");
        when(resourceAccessService.normalizeBizType("TICKET")).thenReturn("TICKET");
        when(resourceAccessService.requireReadAccess("TICKET", 100L, currentUser))
                .thenReturn(new AttachmentResourceScope("TICKET", 100L, 100L));
        when(attachmentMapper.findByBiz("TICKET", 100L)).thenReturn(List.of(textAttachment()));

        var result = attachmentService.search(request, currentUser);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).previewable()).isTrue();
        assertThat(result.get(0).previewType()).isEqualTo("TEXT");
        assertThat(result.get(0).downloadOnly()).isFalse();
        assertThat(result.get(0).downloadUrl()).isEqualTo("/api/files/500/download");
    }

    @Test
    void bindTemporaryAttachmentsShouldRequireOwnedUnboundKnowledgeFiles() {
        Attachment attachment = textAttachment();
        attachment.setBizType("KNOWLEDGE");
        attachment.setBizId(null);
        attachment.setTempToken("knowledge_1234567890abcdef");
        CurrentUser currentUser = user(10L, "AGENT");
        when(resourceAccessService.normalizeBizType("KNOWLEDGE")).thenReturn("KNOWLEDGE");
        when(resourceAccessService.requireWriteAccess("KNOWLEDGE", 200L, currentUser))
                .thenReturn(new AttachmentResourceScope("KNOWLEDGE", 200L, null));
        when(attachmentMapper.findTemporaryByIds("KNOWLEDGE", List.of(500L), 10L)).thenReturn(List.of(attachment));
        when(attachmentMapper.bindTemporaryByIds("KNOWLEDGE", List.of(500L), 10L, 200L, 10L)).thenReturn(1);

        var result = attachmentService.bindTemporaryAttachments("KNOWLEDGE", 200L, List.of("500"), currentUser);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).bizId()).isEqualTo("200");
        assertThat(result.get(0).tempToken()).isNull();
    }

    @Test
    void copyTicketAttachmentsShouldCreateIndependentKnowledgeAttachmentMetadata() {
        Attachment source = textAttachment();
        CurrentUser currentUser = user(10L, "AGENT");
        when(resourceAccessService.requireReadAccess("TICKET", 100L, currentUser))
                .thenReturn(new AttachmentResourceScope("TICKET", 100L, 100L));
        when(resourceAccessService.requireWriteAccess("KNOWLEDGE", 200L, currentUser))
                .thenReturn(new AttachmentResourceScope("KNOWLEDGE", 200L, null));
        when(attachmentMapper.findByBiz("TICKET", 100L)).thenReturn(List.of(source));
        when(attachmentMapper.insert(any(Attachment.class))).thenReturn(1);

        var copied = attachmentService.copyTicketAttachmentsToKnowledge(100L, 200L, currentUser);

        ArgumentCaptor<Attachment> captor = ArgumentCaptor.forClass(Attachment.class);
        verify(attachmentMapper).insert(captor.capture());
        assertThat(captor.getValue().getId()).isNotEqualTo(source.getId());
        assertThat(captor.getValue().getBizType()).isEqualTo("KNOWLEDGE");
        assertThat(captor.getValue().getBizId()).isEqualTo(200L);
        assertThat(captor.getValue().getStoragePath()).isEqualTo(source.getStoragePath());
        assertThat(captor.getValue().getUploaderId()).isEqualTo(10L);
        assertThat(copied).hasSize(1);
        verify(auditLogService).record(10L, "ATTACHMENT_COPY", "ATTACHMENT", 200L,
                "从工单复制附件到知识文章：1 个", null, null);
    }

    @Test
    void logicalDeleteBoundAttachmentsShouldHideAllFilesBeforeBusinessDeletion() {
        CurrentUser currentUser = user(10L, "MANAGER");
        when(resourceAccessService.normalizeBizType("KNOWLEDGE")).thenReturn("KNOWLEDGE");
        when(resourceAccessService.requireWriteAccess("KNOWLEDGE", 200L, currentUser))
                .thenReturn(new AttachmentResourceScope("KNOWLEDGE", 200L, null));
        when(attachmentMapper.logicalDeleteByBiz("KNOWLEDGE", 200L, 10L)).thenReturn(2);

        int affected = attachmentService.logicalDeleteBoundAttachments("KNOWLEDGE", 200L, currentUser);

        assertThat(affected).isEqualTo(2);
        verify(auditLogService).record(10L, "ATTACHMENT_DELETE", "ATTACHMENT", 200L,
                "随业务删除关联附件：2 个", null, null);
    }

    @Test
    void previewShouldEscapeTextContent() {
        Attachment attachment = textAttachment();
        CurrentUser currentUser = user(10L, "USER");
        when(attachmentMapper.findById(500L)).thenReturn(attachment);
        when(resourceAccessService.requireReadAccess("TICKET", 100L, currentUser))
                .thenReturn(new AttachmentResourceScope("TICKET", 100L, 100L));
        when(attachmentStorage.load("2026/06/18/500.txt"))
                .thenReturn(new ByteArrayResource("<script>alert(1)</script>".getBytes(StandardCharsets.UTF_8)));

        var result = attachmentService.preview("500", currentUser);

        assertThat(result.previewType()).isEqualTo("TEXT");
        assertThat(result.content()).isEqualTo("&lt;script&gt;alert(1)&lt;/script&gt;");
        assertThat(result.truncated()).isFalse();
        assertThat(result.resource()).isNull();
    }

    @Test
    void previewShouldRejectDownloadOnlyAttachment() {
        Attachment attachment = textAttachment();
        CurrentUser currentUser = user(10L, "USER");
        attachment.setPreviewable(0);
        attachment.setPreviewType("DOWNLOAD_ONLY");
        attachment.setDownloadOnly(1);
        attachment.setExtension("pdf");
        when(attachmentMapper.findById(500L)).thenReturn(attachment);
        when(resourceAccessService.requireReadAccess("TICKET", 100L, currentUser))
                .thenReturn(new AttachmentResourceScope("TICKET", 100L, 100L));

        assertThatThrownBy(() -> attachmentService.preview("500", currentUser))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.STATE_CONFLICT);
    }

    @Test
    void deleteShouldLogicallyDeleteUploaderAttachmentAndRecordAudit() {
        Attachment attachment = textAttachment();
        CurrentUser currentUser = user(10L, "USER");
        when(attachmentMapper.findById(500L)).thenReturn(attachment);
        when(resourceAccessService.requireWriteAccess("TICKET", 100L, currentUser))
                .thenReturn(new AttachmentResourceScope("TICKET", 100L, 100L));
        when(attachmentMapper.logicalDelete(500L, 10L)).thenReturn(1);
        AttachmentDeleteRequest request = new AttachmentDeleteRequest();
        request.setReason("误传");

        attachmentService.delete("500", request, currentUser, "127.0.0.1", "JUnit");

        verify(attachmentMapper).logicalDelete(500L, 10L);
        verify(auditLogService).record(
                10L,
                "ATTACHMENT_DELETE",
                "ATTACHMENT",
                500L,
                "删除附件：note.txt，原因：误传",
                "127.0.0.1",
                "JUnit"
        );
    }

    private AttachmentUploadRequest uploadRequest(String bizId, String tempToken) {
        AttachmentUploadRequest request = new AttachmentUploadRequest();
        request.setFile(new MockMultipartFile(
                "file",
                "note.txt",
                "text/plain",
                "hello".getBytes(StandardCharsets.UTF_8)
        ));
        request.setBizType("TICKET");
        request.setBizId(bizId);
        request.setTempToken(tempToken);
        return request;
    }

    private Attachment textAttachment() {
        Attachment attachment = new Attachment();
        attachment.setId(500L);
        attachment.setBizType("TICKET");
        attachment.setBizId(100L);
        attachment.setFileName("note.txt");
        attachment.setFileSize(5L);
        attachment.setContentType("text/plain");
        attachment.setExtension("txt");
        attachment.setPreviewable(1);
        attachment.setPreviewType("TEXT");
        attachment.setDownloadOnly(0);
        attachment.setStoragePath("2026/06/18/500.txt");
        attachment.setUploaderId(10L);
        attachment.setUploaderName("张三");
        attachment.setCreateTime(LocalDateTime.of(2026, 6, 18, 10, 0));
        return attachment;
    }

    private CurrentUser user(Long userId, String role) {
        return new CurrentUser(userId, "13800000000", "user" + userId, List.of(role), List.of());
    }
}
