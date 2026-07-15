package com.opsdesk.ticket.service;

import com.opsdesk.audit.service.AuditLogService;
import com.opsdesk.common.exception.BusinessException;
import com.opsdesk.common.exception.ErrorCode;
import com.opsdesk.common.id.SnowflakeIdGenerator;
import com.opsdesk.team.entity.Team;
import com.opsdesk.team.mapper.TeamMapper;
import com.opsdesk.ticket.converter.TicketConverter;
import com.opsdesk.ticket.dto.TicketCategoryMutationRequest;
import com.opsdesk.ticket.entity.TicketCategory;
import com.opsdesk.ticket.mapper.TicketCategoryMapper;
import com.opsdesk.ticket.service.impl.TicketCategoryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 工单分类写能力单元测试。
 *
 * <p>覆盖分类树维护的唯一性、循环、默认团队和删除保护，确保后台配置不会破坏工单分类数据。</p>
 */
@ExtendWith(MockitoExtension.class)
class TicketCategoryServiceImplTest {

    @Mock
    private TicketCategoryMapper ticketCategoryMapper;

    @Mock
    private TeamMapper teamMapper;

    @Mock
    private AuditLogService auditLogService;

    private TicketCategoryServiceImpl ticketCategoryService;

    @BeforeEach
    void setUp() {
        ticketCategoryService = new TicketCategoryServiceImpl(
                ticketCategoryMapper,
                teamMapper,
                new TicketConverter(),
                new SnowflakeIdGenerator(),
                auditLogService
        );
    }

    @Test
    void createShouldRejectDuplicatedSiblingName() {
        TicketCategoryMutationRequest request = mutationRequest("账号问题");
        request.setParentId("1");
        when(ticketCategoryMapper.findById(1L)).thenReturn(category(1L, null, "根分类"));
        when(ticketCategoryMapper.countByParentAndName(1L, "账号问题", null)).thenReturn(1);

        assertThatThrownBy(() -> ticketCategoryService.create(request, 9L, "127.0.0.1", "JUnit"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.STATE_CONFLICT);

        verify(ticketCategoryMapper, never()).insert(any());
    }

    @Test
    void updateShouldRejectDescendantAsParent() {
        when(ticketCategoryMapper.findById(2L)).thenReturn(category(2L, 1L, "系统故障"));
        when(ticketCategoryMapper.countDescendantRelation(2L, 3L)).thenReturn(1);
        TicketCategoryMutationRequest request = mutationRequest("系统故障");
        request.setParentId("3");

        assertThatThrownBy(() -> ticketCategoryService.update("2", request, 9L, "127.0.0.1", "JUnit"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.STATE_CONFLICT);

        verify(ticketCategoryMapper, never()).update(any());
    }

    @Test
    void createShouldRejectDisabledDefaultTeam() {
        Team disabledTeam = new Team();
        disabledTeam.setId(8L);
        disabledTeam.setEnabled(0);
        when(teamMapper.findById(8L)).thenReturn(disabledTeam);
        TicketCategoryMutationRequest request = mutationRequest("网络访问");
        request.setDefaultTeamId("8");

        assertThatThrownBy(() -> ticketCategoryService.create(request, 9L, "127.0.0.1", "JUnit"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.STATE_CONFLICT);

        verify(ticketCategoryMapper, never()).insert(any());
    }

    @Test
    void deleteShouldRejectCategoryWithChildren() {
        when(ticketCategoryMapper.findById(2L)).thenReturn(category(2L, null, "系统故障"));
        when(ticketCategoryMapper.countChildren(2L)).thenReturn(1);

        assertThatThrownBy(() -> ticketCategoryService.delete("2", 9L, "127.0.0.1", "JUnit"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.STATE_CONFLICT);

        verify(ticketCategoryMapper, never()).logicalDelete(any(), any());
    }

    @Test
    void deleteShouldRejectCategoryWithTickets() {
        when(ticketCategoryMapper.findById(2L)).thenReturn(category(2L, null, "系统故障"));
        when(ticketCategoryMapper.countChildren(2L)).thenReturn(0);
        when(ticketCategoryMapper.countTickets(2L)).thenReturn(2);

        assertThatThrownBy(() -> ticketCategoryService.delete("2", 9L, "127.0.0.1", "JUnit"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.STATE_CONFLICT);

        verify(ticketCategoryMapper, never()).logicalDelete(any(), any());
    }

    @Test
    void deleteShouldLogAuditAfterLogicalDelete() {
        when(ticketCategoryMapper.findById(2L)).thenReturn(category(2L, null, "系统故障"));
        when(ticketCategoryMapper.countChildren(2L)).thenReturn(0);
        when(ticketCategoryMapper.countTickets(2L)).thenReturn(0);
        when(ticketCategoryMapper.logicalDelete(2L, 9L)).thenReturn(1);

        ticketCategoryService.delete("2", 9L, "127.0.0.1", "JUnit");

        verify(ticketCategoryMapper).logicalDelete(2L, 9L);
        verify(auditLogService).record(
                eq(9L), eq("TICKET_CATEGORY_DELETE"), eq("TICKET_CATEGORY"), eq(2L),
                eq("删除工单分类：系统故障"), eq("127.0.0.1"), eq("JUnit")
        );
    }

    private TicketCategoryMutationRequest mutationRequest(String name) {
        TicketCategoryMutationRequest request = new TicketCategoryMutationRequest();
        request.setName(name);
        request.setSort(10);
        request.setEnabled(true);
        return request;
    }

    private TicketCategory category(Long id, Long parentId, String name) {
        TicketCategory category = new TicketCategory();
        category.setId(id);
        category.setParentId(parentId);
        category.setName(name);
        category.setSort(10);
        category.setEnabled(1);
        return category;
    }
}
