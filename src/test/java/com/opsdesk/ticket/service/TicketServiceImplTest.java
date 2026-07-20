package com.opsdesk.ticket.service;

import com.opsdesk.common.id.SnowflakeIdGenerator;
import com.opsdesk.common.security.CurrentUser;
import com.opsdesk.ticket.dto.TicketCompleteRequest;
import com.opsdesk.ticket.entity.Ticket;
import com.opsdesk.ticket.enums.TicketStatus;
import com.opsdesk.ticket.mapper.TicketCategoryMapper;
import com.opsdesk.ticket.mapper.TicketMapper;
import com.opsdesk.ticket.mapper.TicketOperationLogMapper;
import com.opsdesk.ticket.mapper.TicketWatchMapper;
import com.opsdesk.ticket.service.impl.TicketServiceImpl;
import com.opsdesk.team.mapper.TeamMemberMapper;
import com.opsdesk.user.mapper.SysUserMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/** 工单完成时结构化解决方案写入规则测试。 */
class TicketServiceImplTest {

    @Test
    void completeShouldPersistStructuredResolution() {
        TicketMapper ticketMapper = mock(TicketMapper.class);
        TicketWatchMapper watchMapper = mock(TicketWatchMapper.class);
        TicketOperationLogMapper operationLogMapper = mock(TicketOperationLogMapper.class);
        TicketStateMachine stateMachine = mock(TicketStateMachine.class);
        Ticket ticket = new Ticket();
        ticket.setId(1L);
        ticket.setStatus("PROCESSING");
        ticket.setCreatorId(2L);
        ticket.setAssigneeId(1L);
        ticket.setResolutionVerified(0);
        when(ticketMapper.findById(1L)).thenReturn(ticket);
        when(ticketMapper.update(any(Ticket.class))).thenReturn(1);
        when(watchMapper.countActive(anyLong(), anyLong())).thenReturn(0);
        when(stateMachine.nextStatus(any(), any(), any())).thenReturn(TicketStatus.PENDING_CONFIRM);
        TicketService service = new TicketServiceImpl(ticketMapper, mock(TicketCategoryMapper.class), operationLogMapper,
                watchMapper, mock(TeamMemberMapper.class), mock(SysUserMapper.class), mock(SnowflakeIdGenerator.class),
                mock(TicketNoGenerator.class), stateMachine);
        TicketCompleteRequest request = new TicketCompleteRequest();
        request.setResolutionSummary("更新 VPN 证书");
        request.setResolutionSteps("1. 替换过期证书\\n2. 重启客户端");
        request.setResolutionVerified(true);

        service.complete("1", request, new CurrentUser(1L, "13800000000", "处理人", List.of("AGENT"), List.of()), "ip", "ua");

        assertEquals("PENDING_CONFIRM", ticket.getStatus());
        assertEquals("更新 VPN 证书", ticket.getResolutionSummary());
        assertEquals("1. 替换过期证书\\n2. 重启客户端", ticket.getResolutionSteps());
        assertEquals(1, ticket.getResolutionVerified());
        verify(operationLogMapper).insert(argThat(log -> log.getContent().contains("解决方案：更新 VPN 证书；已验证")));
    }
}
