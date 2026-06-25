package com.opsdesk.attachment.service;

import com.opsdesk.attachment.mapper.AttachmentMapper;
import com.opsdesk.attachment.model.AttachmentResourceInfo;
import com.opsdesk.common.exception.BusinessException;
import com.opsdesk.common.exception.ErrorCode;
import com.opsdesk.common.security.CurrentUser;
import com.opsdesk.team.mapper.TeamMemberMapper;
import com.opsdesk.ticket.entity.Ticket;
import com.opsdesk.ticket.mapper.TicketMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * 附件业务资源权限测试。
 *
 * <p>附件本身不能绕过工单、评论和知识库资源范围，下载、预览、查询和上传都复用该服务。</p>
 */
@ExtendWith(MockitoExtension.class)
class AttachmentResourceAccessServiceTest {

    @Mock
    private TicketMapper ticketMapper;

    @Mock
    private TeamMemberMapper teamMemberMapper;

    @Mock
    private AttachmentMapper attachmentMapper;

    private AttachmentResourceAccessService accessService;

    @BeforeEach
    void setUp() {
        accessService = new AttachmentResourceAccessService(ticketMapper, teamMemberMapper, attachmentMapper);
    }

    @Test
    void requireReadAccessShouldAllowTicketCreator() {
        when(ticketMapper.findById(100L)).thenReturn(ticket(10L, 20L, 30L));

        var scope = accessService.requireReadAccess("TICKET", 100L, user(10L, "USER"));

        assertThat(scope.ticketId()).isEqualTo(100L);
    }

    @Test
    void requireReadAccessShouldRejectTicketOutsider() {
        when(ticketMapper.findById(100L)).thenReturn(ticket(10L, 20L, 30L));
        when(teamMemberMapper.countActive(30L, 99L)).thenReturn(0);

        assertThatThrownBy(() -> accessService.requireReadAccess("TICKET", 100L, user(99L, "USER")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    void requireReadAccessShouldRejectInternalCommentForOrdinaryTicketCreator() {
        AttachmentResourceInfo comment = new AttachmentResourceInfo();
        comment.setTicketId(100L);
        comment.setOwnerId(20L);
        comment.setStatus("INTERNAL");
        when(attachmentMapper.findCommentResource(200L)).thenReturn(comment);
        when(ticketMapper.findById(100L)).thenReturn(ticket(10L, 20L, 30L));

        assertThatThrownBy(() -> accessService.requireReadAccess("COMMENT", 200L, user(10L, "USER")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    void requireReadAccessShouldAllowPublishedKnowledgeForAuthenticatedUser() {
        AttachmentResourceInfo article = new AttachmentResourceInfo();
        article.setOwnerId(20L);
        article.setStatus("PUBLISHED");
        when(attachmentMapper.findKnowledgeResource(300L)).thenReturn(article);

        var scope = accessService.requireReadAccess("KNOWLEDGE", 300L, user(99L, "USER"));

        assertThat(scope.bizId()).isEqualTo(300L);
        assertThat(scope.ticketId()).isNull();
    }

    private Ticket ticket(Long creatorId, Long assigneeId, Long teamId) {
        Ticket ticket = new Ticket();
        ticket.setId(100L);
        ticket.setCreatorId(creatorId);
        ticket.setAssigneeId(assigneeId);
        ticket.setTeamId(teamId);
        return ticket;
    }

    private CurrentUser user(Long userId, String role) {
        return new CurrentUser(userId, "13800000000", "user" + userId, List.of(role), List.of());
    }
}
