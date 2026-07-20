package com.opsdesk.ticket.service.impl;

import com.opsdesk.attachment.converter.AttachmentConverter;
import com.opsdesk.attachment.entity.Attachment;
import com.opsdesk.attachment.mapper.AttachmentMapper;
import com.opsdesk.common.exception.BusinessException;
import com.opsdesk.common.exception.ErrorCode;
import com.opsdesk.common.id.SnowflakeIdGenerator;
import com.opsdesk.common.security.CurrentUser;
import com.opsdesk.team.mapper.TeamMemberMapper;
import com.opsdesk.ticket.dto.TicketAssignRequest;
import com.opsdesk.ticket.dto.TicketCreateRequest;
import com.opsdesk.ticket.converter.TicketConverter;
import com.opsdesk.ticket.dto.TicketReasonRequest;
import com.opsdesk.ticket.dto.TicketTransferRequest;
import com.opsdesk.ticket.dto.TicketUpdateRequest;
import com.opsdesk.ticket.entity.Ticket;
import com.opsdesk.ticket.entity.TicketCategory;
import com.opsdesk.ticket.entity.TicketOperationLog;
import com.opsdesk.ticket.mapper.TicketCategoryMapper;
import com.opsdesk.ticket.mapper.TicketMapper;
import com.opsdesk.ticket.mapper.TicketOperationLogMapper;
import com.opsdesk.ticket.mapper.TicketWatchMapper;
import com.opsdesk.ticket.service.TicketNoGenerator;
import com.opsdesk.ticket.service.TicketStateMachine;
import com.opsdesk.ticket.vo.TicketVO;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 工单服务单元测试。
 *
 * <p>优先覆盖草稿编号口径和提交状态流转，防止创建接口提前生成工单编号。</p>
 */
@ExtendWith(MockitoExtension.class)
class TicketServiceImplTest {

    @Mock
    private TicketMapper ticketMapper;

    @Mock
    private TicketCategoryMapper ticketCategoryMapper;

    @Mock
    private TicketOperationLogMapper ticketOperationLogMapper;

    @Mock
    private TicketWatchMapper ticketWatchMapper;

    @Mock
    private TeamMemberMapper teamMemberMapper;

    @Mock
    private SysUserMapper sysUserMapper;

    @Mock
    private TicketNoGenerator ticketNoGenerator;

    @Mock
    private AttachmentMapper attachmentMapper;

    private TicketServiceImpl ticketService;

    @BeforeEach
    void setUp() {
        ticketService = new TicketServiceImpl(
                ticketMapper,
                ticketCategoryMapper,
                ticketOperationLogMapper,
                ticketWatchMapper,
                teamMemberMapper,
                sysUserMapper,
                new SnowflakeIdGenerator(),
                ticketNoGenerator,
                new TicketStateMachine(),
                null,
                new TicketConverter(),
                attachmentMapper,
                new AttachmentConverter()
        );
    }

    @Test
    void createShouldKeepTicketNoNullForDraft() {
        when(ticketCategoryMapper.findByIdForUpdate(1L)).thenReturn(category());
        TicketCreateRequest request = createRequest(false);

        TicketVO ticketVO = ticketService.create(request, user(10L, "USER"), "127.0.0.1", "JUnit");

        ArgumentCaptor<Ticket> ticketCaptor = ArgumentCaptor.forClass(Ticket.class);
        verify(ticketMapper).insert(ticketCaptor.capture());
        assertThat(ticketCaptor.getValue().getTicketNo()).isNull();
        assertThat(ticketCaptor.getValue().getStatus()).isEqualTo("DRAFT");
        assertThat(ticketCaptor.getValue().getResolutionVerified()).isZero();
        assertThat(ticketVO.ticketNo()).isNull();
        assertThat(ticketVO.status()).isEqualTo("DRAFT");
        verify(ticketNoGenerator, never()).nextNo();
        verify(ticketOperationLogMapper).insert(any(TicketOperationLog.class));
    }

    @Test
    void createShouldGenerateTicketNoWhenSubmitNow() {
        when(ticketCategoryMapper.findByIdForUpdate(1L)).thenReturn(category());
        when(ticketNoGenerator.nextNo()).thenReturn("TK202606160001");
        TicketCreateRequest request = createRequest(true);

        TicketVO ticketVO = ticketService.create(request, user(10L, "USER"), "127.0.0.1", "JUnit");

        ArgumentCaptor<Ticket> ticketCaptor = ArgumentCaptor.forClass(Ticket.class);
        verify(ticketMapper).insert(ticketCaptor.capture());
        assertThat(ticketCaptor.getValue().getTicketNo()).isEqualTo("TK202606160001");
        assertThat(ticketCaptor.getValue().getStatus()).isEqualTo("PENDING_ASSIGN");
        assertThat(ticketVO.ticketNo()).isEqualTo("TK202606160001");
        assertThat(ticketVO.status()).isEqualTo("PENDING_ASSIGN");
    }

    @Test
    void createShouldLockCategoryBeforeWritingTicketReference() {
        when(ticketCategoryMapper.findByIdForUpdate(1L)).thenReturn(category());

        ticketService.create(createRequest(false), user(10L, "USER"), "127.0.0.1", "JUnit");

        var order = org.mockito.Mockito.inOrder(ticketCategoryMapper, ticketMapper);
        order.verify(ticketCategoryMapper).findByIdForUpdate(1L);
        order.verify(ticketMapper).insert(any(Ticket.class));
    }

    @Test
    void updateDraftShouldLockNewCategoryBeforeChangingReference() {
        when(ticketMapper.findById(100L)).thenReturn(draftTicket(10L));
        when(ticketCategoryMapper.findByIdForUpdate(1L)).thenReturn(category());
        when(ticketMapper.update(any(Ticket.class))).thenReturn(1);
        TicketUpdateRequest request = new TicketUpdateRequest();
        request.setTitle("无法登录系统");
        request.setDescription("登录时报错，请协助排查。");
        request.setCategoryId("1");
        request.setPriority("MEDIUM");

        ticketService.updateDraft("100", request, user(10L, "USER"), "127.0.0.1", "JUnit");

        var order = org.mockito.Mockito.inOrder(ticketCategoryMapper, ticketMapper);
        order.verify(ticketCategoryMapper).findByIdForUpdate(1L);
        order.verify(ticketMapper).update(any(Ticket.class));
    }

    @Test
    void submitShouldGenerateTicketNoAndMoveToPendingAssign() {
        when(ticketMapper.findById(100L)).thenReturn(draftTicket(10L));
        when(ticketCategoryMapper.findByIdForUpdate(1L)).thenReturn(category());
        when(ticketNoGenerator.nextNo()).thenReturn("TK202606160001");
        when(ticketMapper.update(any(Ticket.class))).thenReturn(1);

        TicketVO ticketVO = ticketService.submit("100", user(10L, "USER"), "127.0.0.1", "JUnit");

        ArgumentCaptor<Ticket> ticketCaptor = ArgumentCaptor.forClass(Ticket.class);
        verify(ticketMapper).update(ticketCaptor.capture());
        assertThat(ticketCaptor.getValue().getTicketNo()).isEqualTo("TK202606160001");
        assertThat(ticketCaptor.getValue().getStatus()).isEqualTo("PENDING_ASSIGN");
        assertThat(ticketVO.ticketNo()).isEqualTo("TK202606160001");
        assertThat(ticketVO.status()).isEqualTo("PENDING_ASSIGN");
    }

    @Test
    void submitShouldRejectNonCreator() {
        when(ticketMapper.findById(100L)).thenReturn(draftTicket(10L));

        assertThatThrownBy(() -> ticketService.submit("100", user(20L, "USER"), "127.0.0.1", "JUnit"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FORBIDDEN);

        verify(ticketMapper, never()).update(any());
    }

    @Test
    void detailShouldIncludeTicketAttachments() {
        when(ticketMapper.findById(100L)).thenReturn(draftTicket(10L));
        when(attachmentMapper.findByBiz("TICKET", 100L)).thenReturn(List.of(attachment()));

        TicketVO ticketVO = ticketService.detail("100", user(10L, "USER"));

        assertThat(ticketVO.attachments()).hasSize(1);
        assertThat(ticketVO.attachments().get(0).id()).isEqualTo("500");
        assertThat(ticketVO.attachments().get(0).downloadUrl()).isEqualTo("/api/files/500/download");
    }

    @Test
    void detailShouldReturnAcceptActionForAssignedUserWithoutAgentRole() {
        when(ticketMapper.findById(100L)).thenReturn(pendingProcessTicket(10L, 20L));

        TicketVO ticketVO = ticketService.detail("100", user(20L, "USER"));

        assertThat(ticketVO.availableActions()).containsExactly("accept", "reject");
    }

    @Test
    void acceptShouldAllowAssignedUserWithoutAgentRole() {
        when(ticketMapper.findById(100L)).thenReturn(pendingProcessTicket(10L, 20L));
        when(ticketMapper.update(any(Ticket.class))).thenReturn(1);

        TicketVO ticketVO = ticketService.accept("100", user(20L, "USER"), "127.0.0.1", "JUnit");

        ArgumentCaptor<Ticket> ticketCaptor = ArgumentCaptor.forClass(Ticket.class);
        verify(ticketMapper).update(ticketCaptor.capture());
        assertThat(ticketCaptor.getValue().getStatus()).isEqualTo("PROCESSING");
        assertThat(ticketCaptor.getValue().getAssigneeId()).isEqualTo(20L);
        assertThat(ticketVO.status()).isEqualTo("PROCESSING");
        assertThat(ticketVO.availableActions()).containsExactly("reject", "complete");
    }

    @Test
    void transferShouldRejectAssignedUserWithoutAgentRole() {
        when(ticketMapper.findById(100L)).thenReturn(pendingProcessTicket(10L, 20L));
        when(teamMemberMapper.countActive(1L, 30L)).thenReturn(1);
        TicketTransferRequest request = transferRequest("1", "30");

        assertThatThrownBy(() -> ticketService.transfer("100", request, user(20L, "USER"), "127.0.0.1", "JUnit"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FORBIDDEN);

        verify(ticketMapper, never()).update(any());
    }

    @Test
    void rejectShouldReturnAssignedUserTicketToPendingAssign() {
        when(ticketMapper.findById(100L)).thenReturn(processingTicket(10L, 20L));
        when(ticketMapper.update(any(Ticket.class))).thenReturn(1);

        TicketVO ticketVO = ticketService.reject("100", reasonRequest("退回团队负责人"), user(20L, "USER"), "127.0.0.1", "JUnit");

        ArgumentCaptor<Ticket> ticketCaptor = ArgumentCaptor.forClass(Ticket.class);
        verify(ticketMapper).update(ticketCaptor.capture());
        assertThat(ticketCaptor.getValue().getStatus()).isEqualTo("PENDING_ASSIGN");
        assertThat(ticketCaptor.getValue().getAssigneeId()).isNull();
        assertThat(ticketVO.status()).isEqualTo("PENDING_ASSIGN");
    }

    @Test
    void assignShouldAllowTeamOnlyWithoutAssignee() {
        when(ticketMapper.findById(100L)).thenReturn(pendingAssignTicket(10L, null));
        when(ticketMapper.update(any(Ticket.class))).thenReturn(1);
        TicketAssignRequest request = assignRequest("2", null);

        TicketVO ticketVO = ticketService.assign("100", request, user(1L, "ADMIN"), "127.0.0.1", "JUnit");

        ArgumentCaptor<Ticket> ticketCaptor = ArgumentCaptor.forClass(Ticket.class);
        verify(ticketMapper).update(ticketCaptor.capture());
        assertThat(ticketCaptor.getValue().getTeamId()).isEqualTo(2L);
        assertThat(ticketCaptor.getValue().getAssigneeId()).isNull();
        assertThat(ticketCaptor.getValue().getStatus()).isEqualTo("PENDING_PROCESS");
        assertThat(ticketVO.teamId()).isEqualTo("2");
        assertThat(ticketVO.assigneeId()).isNull();
    }

    @Test
    void assignShouldRejectAssigneeOutsideSelectedTeam() {
        when(ticketMapper.findById(100L)).thenReturn(pendingAssignTicket(10L, null));
        when(teamMemberMapper.countActive(2L, 30L)).thenReturn(0);
        TicketAssignRequest request = assignRequest("2", "30");

        assertThatThrownBy(() -> ticketService.assign("100", request, user(1L, "ADMIN"), "127.0.0.1", "JUnit"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PARAM_ERROR);

        verify(ticketMapper, never()).update(any());
    }

    private TicketCreateRequest createRequest(boolean submitNow) {
        TicketCreateRequest request = new TicketCreateRequest();
        request.setTitle("无法登录系统");
        request.setDescription("登录时报错，请协助排查。");
        request.setCategoryId("1");
        request.setPriority("MEDIUM");
        request.setTags(List.of("登录", "账号"));
        request.setSubmitNow(submitNow);
        return request;
    }

    private TicketCategory category() {
        TicketCategory category = new TicketCategory();
        category.setId(1L);
        category.setName("账号问题");
        category.setEnabled(1);
        category.setDefaultSlaHours(24);
        return category;
    }

    private Ticket draftTicket(Long creatorId) {
        Ticket ticket = new Ticket();
        ticket.setId(100L);
        ticket.setTitle("无法登录系统");
        ticket.setDescription("登录时报错，请协助排查。");
        ticket.setCategoryId(1L);
        ticket.setPriority("MEDIUM");
        ticket.setStatus("DRAFT");
        ticket.setCreatorId(creatorId);
        ticket.setCreateTime(LocalDateTime.now());
        ticket.setUpdateTime(LocalDateTime.now());
        return ticket;
    }

    private Ticket pendingAssignTicket(Long creatorId, Long teamId) {
        Ticket ticket = draftTicket(creatorId);
        ticket.setTicketNo("TK202606160001");
        ticket.setStatus("PENDING_ASSIGN");
        ticket.setTeamId(teamId);
        return ticket;
    }

    private Ticket pendingProcessTicket(Long creatorId, Long assigneeId) {
        Ticket ticket = draftTicket(creatorId);
        ticket.setTicketNo("TK202606160001");
        ticket.setStatus("PENDING_PROCESS");
        ticket.setTeamId(1L);
        ticket.setAssigneeId(assigneeId);
        return ticket;
    }

    private Ticket processingTicket(Long creatorId, Long assigneeId) {
        Ticket ticket = pendingProcessTicket(creatorId, assigneeId);
        ticket.setStatus("PROCESSING");
        return ticket;
    }

    private TicketAssignRequest assignRequest(String teamId, String assigneeId) {
        TicketAssignRequest request = new TicketAssignRequest();
        request.setTeamId(teamId);
        request.setAssigneeId(assigneeId);
        request.setReason("assign by team");
        return request;
    }

    private TicketTransferRequest transferRequest(String teamId, String assigneeId) {
        TicketTransferRequest request = new TicketTransferRequest();
        request.setTargetTeamId(teamId);
        request.setTargetAssigneeId(assigneeId);
        request.setReason("transfer by team");
        return request;
    }

    private TicketReasonRequest reasonRequest(String reason) {
        TicketReasonRequest request = new TicketReasonRequest();
        request.setReason(reason);
        return request;
    }

    private Attachment attachment() {
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
