package com.opsdesk.comment.service.impl;

import com.opsdesk.attachment.mapper.AttachmentMapper;
import com.opsdesk.audit.service.AuditLogService;
import com.opsdesk.comment.dto.CommentCreateRequest;
import com.opsdesk.comment.dto.CommentSearchRequest;
import com.opsdesk.comment.entity.TicketComment;
import com.opsdesk.comment.mapper.TicketCommentMapper;
import com.opsdesk.common.exception.BusinessException;
import com.opsdesk.common.exception.ErrorCode;
import com.opsdesk.common.id.SnowflakeIdGenerator;
import com.opsdesk.common.response.PageResult;
import com.opsdesk.common.security.CurrentUser;
import com.opsdesk.team.mapper.TeamMemberMapper;
import com.opsdesk.ticket.entity.Ticket;
import com.opsdesk.ticket.entity.TicketOperationLog;
import com.opsdesk.ticket.mapper.TicketMapper;
import com.opsdesk.ticket.mapper.TicketOperationLogMapper;
import com.opsdesk.user.mapper.SysUserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 工单评论服务测试。
 *
 * <p>覆盖评论资源范围、内部备注可见性、逻辑删除和临时附件绑定，避免协作接口绕过工单权限。</p>
 */
@ExtendWith(MockitoExtension.class)
class TicketCommentServiceImplTest {

    @Mock
    private TicketCommentMapper commentMapper;

    @Mock
    private TicketMapper ticketMapper;

    @Mock
    private TicketOperationLogMapper ticketOperationLogMapper;

    @Mock
    private TeamMemberMapper teamMemberMapper;

    @Mock
    private SysUserMapper sysUserMapper;

    @Mock
    private AttachmentMapper attachmentMapper;

    @Mock
    private AuditLogService auditLogService;

    private TicketCommentServiceImpl commentService;

    @BeforeEach
    void setUp() {
        commentService = new TicketCommentServiceImpl(
                commentMapper,
                ticketMapper,
                ticketOperationLogMapper,
                teamMemberMapper,
                sysUserMapper,
                attachmentMapper,
                new SnowflakeIdGenerator(),
                auditLogService
        );
    }

    @Test
    void createShouldRejectUserOutsideTicketScope() {
        when(ticketMapper.findById(100L)).thenReturn(ticket(10L, 20L, 30L));
        when(teamMemberMapper.countActive(30L, 99L)).thenReturn(0);

        CommentCreateRequest request = new CommentCreateRequest();
        request.setContent("请协助排查登录失败。");
        request.setCommentType("PUBLIC");

        assertThatThrownBy(() -> commentService.create("100", request, user(99L, "USER"), "127.0.0.1", "JUnit"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FORBIDDEN);

        verify(commentMapper, never()).insert(any());
    }

    @Test
    void searchShouldHideInternalCommentsForCreatorUser() {
        when(ticketMapper.findById(100L)).thenReturn(ticket(10L, 20L, 30L));
        when(commentMapper.countByTicketId(100L, false)).thenReturn(1L);
        when(commentMapper.searchByTicketId(100L, false, 0L, 20L)).thenReturn(List.of(publicComment()));

        PageResult<?> result = commentService.search("100", new CommentSearchRequest(), user(10L, "USER"));

        assertThat(result.total()).isEqualTo(1);
        verify(commentMapper).countByTicketId(100L, false);
        verify(commentMapper).searchByTicketId(100L, false, 0L, 20L);
    }

    @Test
    void createShouldBindTempAttachmentsToComment() {
        when(ticketMapper.findById(100L)).thenReturn(ticket(10L, 20L, 30L));
        when(commentMapper.insert(any())).thenReturn(1);
        when(attachmentMapper.bindTempToBiz(eq("COMMENT"), eq("temp-token-123456"), eq(10L), any(Long.class), eq(10L))).thenReturn(2);

        CommentCreateRequest request = new CommentCreateRequest();
        request.setContent("已补充排查日志。");
        request.setCommentType("PUBLIC");
        request.setTempToken("temp-token-123456");

        commentService.create("100", request, user(10L, "USER"), "127.0.0.1", "JUnit");

        ArgumentCaptor<TicketComment> commentCaptor = ArgumentCaptor.forClass(TicketComment.class);
        verify(commentMapper).insert(commentCaptor.capture());
        verify(attachmentMapper).bindTempToBiz("COMMENT", "temp-token-123456", 10L, commentCaptor.getValue().getId(), 10L);
    }

    @Test
    void deleteShouldAllowAuthorAndWriteAudit() {
        TicketComment comment = publicComment();
        when(commentMapper.findById(200L)).thenReturn(comment);
        when(ticketMapper.findById(100L)).thenReturn(ticket(10L, 20L, 30L));
        when(commentMapper.logicalDelete(200L, 10L)).thenReturn(1);

        commentService.delete("200", user(10L, "USER"), "127.0.0.1", "JUnit");

        verify(commentMapper).logicalDelete(200L, 10L);
        verify(ticketOperationLogMapper).insert(any(TicketOperationLog.class));
        verify(auditLogService).record(10L, "COMMENT_DELETE", "COMMENT", 200L, "删除工单评论", "127.0.0.1", "JUnit");
    }

    private Ticket ticket(Long creatorId, Long assigneeId, Long teamId) {
        Ticket ticket = new Ticket();
        ticket.setId(100L);
        ticket.setTitle("无法登录系统");
        ticket.setCreatorId(creatorId);
        ticket.setAssigneeId(assigneeId);
        ticket.setTeamId(teamId);
        ticket.setStatus("PROCESSING");
        return ticket;
    }

    private TicketComment publicComment() {
        TicketComment comment = new TicketComment();
        comment.setId(200L);
        comment.setTicketId(100L);
        comment.setContent("请查看日志。");
        comment.setCommentType("PUBLIC");
        comment.setAuthorId(10L);
        comment.setCreateTime(LocalDateTime.of(2026, 6, 30, 10, 0));
        comment.setUpdateTime(comment.getCreateTime());
        return comment;
    }

    private CurrentUser user(Long userId, String role) {
        return new CurrentUser(userId, "13800000000", "user" + userId, List.of(role), List.of());
    }
}
