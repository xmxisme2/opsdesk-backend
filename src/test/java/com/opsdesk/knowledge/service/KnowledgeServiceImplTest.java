package com.opsdesk.knowledge.service;

import com.opsdesk.audit.service.AuditLogService;
import com.opsdesk.attachment.service.AttachmentService;
import com.opsdesk.comment.mapper.TicketCommentMapper;
import com.opsdesk.common.exception.BusinessException;
import com.opsdesk.common.id.SnowflakeIdGenerator;
import com.opsdesk.common.security.CurrentUser;
import com.opsdesk.knowledge.converter.KnowledgeConverter;
import com.opsdesk.knowledge.dto.KnowledgeArticleSearchRequest;
import com.opsdesk.knowledge.dto.KnowledgeFromTicketRequest;
import com.opsdesk.knowledge.entity.KnowledgeArticle;
import com.opsdesk.knowledge.mapper.KnowledgeArticleMapper;
import com.opsdesk.knowledge.mapper.KnowledgeArticleRow;
import com.opsdesk.knowledge.mapper.KnowledgeCategoryMapper;
import com.opsdesk.knowledge.mapper.KnowledgeTagMapper;
import com.opsdesk.knowledge.service.impl.KnowledgeServiceImpl;
import com.opsdesk.ticket.entity.Ticket;
import com.opsdesk.ticket.mapper.TicketMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** 知识库服务权限、状态和工单草稿规则测试。 */
class KnowledgeServiceImplTest {
    private KnowledgeArticleMapper articleMapper;
    private TicketMapper ticketMapper;
    private AttachmentService attachmentService;
    private KnowledgeService service;

    @BeforeEach
    void setUp() {
        articleMapper = mock(KnowledgeArticleMapper.class);
        ticketMapper = mock(TicketMapper.class);
        attachmentService = mock(AttachmentService.class);
        service = new KnowledgeServiceImpl(articleMapper, mock(KnowledgeCategoryMapper.class), mock(KnowledgeTagMapper.class),
                ticketMapper, mock(TicketCommentMapper.class), new KnowledgeConverter(), mock(SnowflakeIdGenerator.class), mock(AuditLogService.class), attachmentService);
    }

    @Test
    void normalUserSearchShouldForcePublishedStatus() {
        when(articleMapper.search(any(), any(), any(), any(), any(), any())).thenReturn(List.of());
        service.search(new KnowledgeArticleSearchRequest(), user("USER"));
        ArgumentCaptor<String> status = ArgumentCaptor.forClass(String.class);
        verify(articleMapper).search(isNull(), isNull(), isNull(), status.capture(), isNull(), eq("updatedAt"));
        assertEquals("PUBLISHED", status.getValue());
    }

    @Test
    void normalUserCannotReadDraft() {
        KnowledgeArticleRow row = article(2L, "DRAFT", 9L);
        when(articleMapper.findById(2L)).thenReturn(row);
        BusinessException error = assertThrows(BusinessException.class, () -> service.detail("2", user("USER")));
        assertEquals(403001, error.getErrorCode().getCode());
    }

    @Test
    void publishedDetailIncrementsViewCount() {
        KnowledgeArticleRow row = article(2L, "PUBLISHED", 9L);
        row.setViewCount(5L);
        when(articleMapper.findById(2L)).thenReturn(row);
        assertEquals(6, service.detail("2", user("USER")).getViewCount());
        verify(articleMapper).incrementViewCount(2L);
        verify(attachmentService).search(any(), any());
    }

    @Test
    void fromTicketRejectsNonTerminalTicket() {
        Ticket ticket = new Ticket(); ticket.setId(3L); ticket.setStatus("PROCESSING");
        when(ticketMapper.findById(3L)).thenReturn(ticket);
        assertThrows(BusinessException.class, () -> service.fromTicket("3", new KnowledgeFromTicketRequest(), user("AGENT"), "ip", "ua"));
        verify(articleMapper, never()).insert(any(KnowledgeArticle.class));
    }

    @Test
    void fromTicketCopiesDirectAttachmentsWhenRequested() {
        Ticket ticket = new Ticket(); ticket.setId(3L); ticket.setStatus("CLOSED"); ticket.setTitle("VPN 故障"); ticket.setDescription("无法连接");
        KnowledgeArticleRow draft = article(9L, "DRAFT", 1L);
        when(ticketMapper.findById(3L)).thenReturn(ticket);
        when(articleMapper.findById(9L)).thenReturn(draft);
        when(articleMapper.insert(any(KnowledgeArticle.class))).thenReturn(1);
        when(attachmentService.search(any(), any())).thenReturn(List.of());
        SnowflakeIdGenerator idGenerator = mock(SnowflakeIdGenerator.class);
        when(idGenerator.nextId()).thenReturn(9L);
        service = new KnowledgeServiceImpl(articleMapper, mock(KnowledgeCategoryMapper.class), mock(KnowledgeTagMapper.class),
                ticketMapper, mock(TicketCommentMapper.class), new KnowledgeConverter(), idGenerator, mock(AuditLogService.class), attachmentService);

        service.fromTicket("3", new KnowledgeFromTicketRequest(), user("AGENT"), "ip", "ua");

        verify(attachmentService).copyTicketAttachmentsToKnowledge(eq(3L), eq(9L), any(CurrentUser.class));
    }

    @Test
    void fromTicketSkipsAttachmentCopyWhenNotRequestedButStillVerifiesTicketAccess() {
        Ticket ticket = new Ticket(); ticket.setId(3L); ticket.setStatus("COMPLETED"); ticket.setTitle("VPN 故障"); ticket.setDescription("已恢复");
        KnowledgeArticleRow draft = article(9L, "DRAFT", 1L);
        KnowledgeFromTicketRequest request = new KnowledgeFromTicketRequest(); request.setIncludeAttachments(false);
        when(ticketMapper.findById(3L)).thenReturn(ticket);
        when(articleMapper.findById(9L)).thenReturn(draft);
        when(articleMapper.insert(any(KnowledgeArticle.class))).thenReturn(1);
        when(attachmentService.search(any(), any())).thenReturn(List.of());
        SnowflakeIdGenerator idGenerator = mock(SnowflakeIdGenerator.class);
        when(idGenerator.nextId()).thenReturn(9L);
        service = new KnowledgeServiceImpl(articleMapper, mock(KnowledgeCategoryMapper.class), mock(KnowledgeTagMapper.class),
                ticketMapper, mock(TicketCommentMapper.class), new KnowledgeConverter(), idGenerator, mock(AuditLogService.class), attachmentService);

        service.fromTicket("3", request, user("AGENT"), "ip", "ua");

        verify(attachmentService, never()).copyTicketAttachmentsToKnowledge(anyLong(), anyLong(), any());
        verify(attachmentService, atLeastOnce()).search(any(), any());
    }

    private CurrentUser user(String role) { return new CurrentUser(1L, "13800000000", "测试用户", List.of(role), List.of()); }
    private KnowledgeArticleRow article(Long id, String status, Long authorId) {
        KnowledgeArticleRow row = new KnowledgeArticleRow(); row.setId(id); row.setStatus(status); row.setAuthorId(authorId);
        row.setTitle("测试文章"); row.setContent("正文"); return row;
    }
}
