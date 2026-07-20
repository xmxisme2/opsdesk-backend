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
import org.springframework.dao.DuplicateKeyException;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 工单分类写能力单元测试。
 *
 * <p>覆盖唯一性、循环、默认团队、数据库时间回读、审计和基于行锁的删除竞态保护。</p>
 */
@ExtendWith(MockitoExtension.class)
class TicketCategoryServiceImplTest {

    @Mock
    private TicketCategoryMapper ticketCategoryMapper;

    @Mock
    private TeamMapper teamMapper;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private SnowflakeIdGenerator idGenerator;

    private TicketCategoryServiceImpl ticketCategoryService;

    @BeforeEach
    void setUp() {
        ticketCategoryService = new TicketCategoryServiceImpl(
                ticketCategoryMapper, teamMapper, new TicketConverter(), idGenerator, auditLogService);
    }

    @Test
    void createShouldRejectDuplicatedSiblingName() {
        TicketCategoryMutationRequest request = mutationRequest("账号问题");
        request.setParentId("1");
        when(ticketCategoryMapper.findByIdForUpdate(1L)).thenReturn(category(1L, null, "根分类"));
        when(ticketCategoryMapper.countByParentAndName(1L, "账号问题", null)).thenReturn(1);

        assertThatThrownBy(() -> ticketCategoryService.create(request, 9L, "127.0.0.1", "JUnit"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.STATE_CONFLICT);
        verify(ticketCategoryMapper, never()).insert(any());
    }

    @Test
    void updateShouldRejectDescendantAsParent() {
        when(ticketCategoryMapper.findByIdsForUpdate(java.util.List.of(2L, 3L))).thenReturn(java.util.List.of(
                category(2L, 1L, "系统故障"), category(3L, 2L, "应用故障")));
        when(ticketCategoryMapper.countDescendantRelation(2L, 3L)).thenReturn(1);
        TicketCategoryMutationRequest request = mutationRequest("系统故障");
        request.setParentId("3");

        assertThatThrownBy(() -> ticketCategoryService.update("2", request, 9L, "127.0.0.1", "JUnit"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.STATE_CONFLICT);
        verify(ticketCategoryMapper, never()).update(any());
    }

    @Test
    void updateShouldRejectSelfAsParent() {
        TicketCategoryMutationRequest request = mutationRequest("系统故障");
        request.setParentId("2");

        assertThatThrownBy(() -> ticketCategoryService.update("2", request, 9L, "127.0.0.1", "JUnit"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.STATE_CONFLICT);
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
                .extracting("errorCode").isEqualTo(ErrorCode.STATE_CONFLICT);
    }

    @Test
    void createShouldRejectMissingDefaultTeam() {
        when(teamMapper.findById(8L)).thenReturn(null);
        TicketCategoryMutationRequest request = mutationRequest("网络访问");
        request.setDefaultTeamId("8");

        assertThatThrownBy(() -> ticketCategoryService.create(request, 9L, "127.0.0.1", "JUnit"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.NOT_FOUND);
    }

    @Test
    void createShouldRejectNonPositiveDefaultSla() {
        TicketCategoryMutationRequest request = mutationRequest("网络访问");
        request.setDefaultSlaHours(0);

        assertThatThrownBy(() -> ticketCategoryService.create(request, 9L, "127.0.0.1", "JUnit"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.PARAM_ERROR);
    }

    @Test
    void createShouldReloadDatabaseTimesAndUseSystemConfigAuditType() {
        when(idGenerator.nextId()).thenReturn(100L);
        TicketCategory persisted = category(100L, null, "账号问题");
        persisted.setCreateTime(LocalDateTime.of(2026, 7, 15, 9, 0));
        persisted.setUpdateTime(LocalDateTime.of(2026, 7, 15, 9, 0));
        when(ticketCategoryMapper.findById(100L)).thenReturn(persisted);

        var result = ticketCategoryService.create(mutationRequest("账号问题"), 9L, "127.0.0.1", "JUnit");

        assertThat(result.createdAt()).isEqualTo("2026-07-15 09:00:00");
        assertThat(result.updatedAt()).isEqualTo("2026-07-15 09:00:00");
        verify(auditLogService).record(9L, "TICKET_CATEGORY_CREATE", "SYSTEM_CONFIG", 100L,
                "创建工单分类：账号问题", "127.0.0.1", "JUnit");
    }

    @Test
    void updateShouldReloadDatabaseTimeAndUseSystemConfigAuditType() {
        when(ticketCategoryMapper.findByIdForUpdate(2L)).thenReturn(category(2L, null, "系统故障"));
        TicketCategory persisted = category(2L, null, "应用故障");
        persisted.setCreateTime(LocalDateTime.of(2026, 7, 14, 9, 0));
        persisted.setUpdateTime(LocalDateTime.of(2026, 7, 15, 10, 30));
        when(ticketCategoryMapper.findById(2L)).thenReturn(persisted);
        when(ticketCategoryMapper.update(any())).thenReturn(1);

        var result = ticketCategoryService.update("2", mutationRequest("应用故障"),
                9L, "127.0.0.1", "JUnit");

        assertThat(result.updatedAt()).isEqualTo("2026-07-15 10:30:00");
        verify(auditLogService).record(9L, "TICKET_CATEGORY_UPDATE", "SYSTEM_CONFIG", 2L,
                "编辑工单分类：应用故障", "127.0.0.1", "JUnit");
    }

    @Test
    void updateShouldLockCurrentAndParentInAscendingIdOrder() {
        when(ticketCategoryMapper.findByIdsForUpdate(java.util.List.of(2L, 5L))).thenReturn(java.util.List.of(
                category(2L, null, "目标父分类"), category(5L, null, "待移动分类")));
        when(ticketCategoryMapper.update(any())).thenReturn(1);
        when(ticketCategoryMapper.findById(5L)).thenReturn(category(5L, 2L, "待移动分类"));
        TicketCategoryMutationRequest request = mutationRequest("待移动分类");
        request.setParentId("2");

        ticketCategoryService.update("5", request, 9L, "127.0.0.1", "JUnit");

        verify(ticketCategoryMapper).findByIdsForUpdate(java.util.List.of(2L, 5L));
    }

    @Test
    void createShouldTranslateDatabaseUniqueConflict() {
        when(idGenerator.nextId()).thenReturn(100L);
        when(ticketCategoryMapper.insert(any())).thenThrow(new DuplicateKeyException("duplicate"));

        assertThatThrownBy(() -> ticketCategoryService.create(
                mutationRequest("账号问题"), 9L, "127.0.0.1", "JUnit"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.STATE_CONFLICT);
    }

    @Test
    void updateShouldTranslateDatabaseUniqueConflict() {
        when(ticketCategoryMapper.findByIdForUpdate(2L)).thenReturn(category(2L, null, "系统故障"));
        when(ticketCategoryMapper.update(any())).thenThrow(new DuplicateKeyException("duplicate"));

        assertThatThrownBy(() -> ticketCategoryService.update(
                "2", mutationRequest("账号问题"), 9L, "127.0.0.1", "JUnit"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.STATE_CONFLICT);
    }

    @Test
    void deleteShouldRejectCategoryWithChildren() {
        when(ticketCategoryMapper.findByIdForUpdate(2L)).thenReturn(category(2L, null, "系统故障"));
        when(ticketCategoryMapper.countChildren(2L)).thenReturn(1);

        assertThatThrownBy(() -> ticketCategoryService.delete("2", 9L, "127.0.0.1", "JUnit"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.STATE_CONFLICT);
        verify(ticketCategoryMapper, never()).logicalDelete(any(), any());
    }

    @Test
    void deleteShouldRejectCategoryWithTickets() {
        when(ticketCategoryMapper.findByIdForUpdate(2L)).thenReturn(category(2L, null, "系统故障"));
        when(ticketCategoryMapper.countChildren(2L)).thenReturn(0);
        when(ticketCategoryMapper.countTickets(2L)).thenReturn(2);

        assertThatThrownBy(() -> ticketCategoryService.delete("2", 9L, "127.0.0.1", "JUnit"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.STATE_CONFLICT);
        verify(ticketCategoryMapper, never()).logicalDelete(any(), any());
    }

    @Test
    void deleteShouldLogAuditAfterLogicalDelete() {
        when(ticketCategoryMapper.findByIdForUpdate(2L)).thenReturn(category(2L, null, "系统故障"));
        when(ticketCategoryMapper.countChildren(2L)).thenReturn(0);
        when(ticketCategoryMapper.countTickets(2L)).thenReturn(0);
        when(ticketCategoryMapper.logicalDelete(2L, 9L)).thenReturn(1);

        ticketCategoryService.delete("2", 9L, "127.0.0.1", "JUnit");

        verify(auditLogService).record(eq(9L), eq("TICKET_CATEGORY_DELETE"), eq("SYSTEM_CONFIG"), eq(2L),
                eq("删除工单分类：系统故障"), eq("127.0.0.1"), eq("JUnit"));
    }

    @Test
    void deleteShouldHoldCategoryLockBeforeRelationChecks() {
        when(ticketCategoryMapper.findByIdForUpdate(2L)).thenReturn(category(2L, null, "系统故障"));
        when(ticketCategoryMapper.countChildren(2L)).thenReturn(0);
        when(ticketCategoryMapper.countTickets(2L)).thenReturn(0);
        when(ticketCategoryMapper.logicalDelete(2L, 9L)).thenReturn(1);

        ticketCategoryService.delete("2", 9L, "127.0.0.1", "JUnit");

        var order = inOrder(ticketCategoryMapper);
        order.verify(ticketCategoryMapper).findByIdForUpdate(2L);
        order.verify(ticketCategoryMapper).countChildren(2L);
        order.verify(ticketCategoryMapper).countTickets(2L);
        order.verify(ticketCategoryMapper).logicalDelete(2L, 9L);
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
