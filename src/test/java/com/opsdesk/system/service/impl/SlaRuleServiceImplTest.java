package com.opsdesk.system.service.impl;

import com.opsdesk.audit.service.AuditLogService;
import com.opsdesk.common.exception.BusinessException;
import com.opsdesk.common.id.SnowflakeIdGenerator;
import com.opsdesk.system.converter.SlaRuleConverter;
import com.opsdesk.system.dto.SlaRuleMutationRequest;
import com.opsdesk.system.entity.SlaRule;
import com.opsdesk.system.mapper.SlaRuleMapper;
import com.opsdesk.ticket.entity.TicketCategory;
import com.opsdesk.ticket.mapper.TicketCategoryMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** SLA 规则服务核心约束测试。 */
class SlaRuleServiceImplTest {
    @Test
    void shouldCreateRuleAndRecordAuditLog() {
        SlaRuleMapper mapper = mock(SlaRuleMapper.class);
        TicketCategoryMapper categoryMapper = mock(TicketCategoryMapper.class);
        AuditLogService auditLogService = mock(AuditLogService.class);
        SnowflakeIdGenerator idGenerator = mock(SnowflakeIdGenerator.class);
        when(categoryMapper.findById(1L)).thenReturn(new TicketCategory());
        when(idGenerator.nextId()).thenReturn(100L);
        when(mapper.findById(100L)).thenAnswer(invocation -> {
            SlaRule rule = new SlaRule(); rule.setId(100L); rule.setCategoryId(1L); rule.setPriority("HIGH");
            rule.setResponseHours(2); rule.setResolveHours(12); rule.setEnabled(1); return rule;
        });
        SlaRuleServiceImpl service = new SlaRuleServiceImpl(mapper, categoryMapper, auditLogService, idGenerator, new SlaRuleConverter());

        var result = service.create(request(2, 12), 1L, "127.0.0.1", "test");

        assertThat(result.id()).isEqualTo("100");
        verify(mapper).insert(any(SlaRule.class));
        verify(auditLogService).record(eq(1L), eq("CREATE"), eq("SYSTEM_CONFIG"), eq(100L), any(), eq("127.0.0.1"), eq("test"));
    }

    @Test
    void shouldRejectResolveHoursLessThanResponseHours() {
        TicketCategoryMapper categoryMapper = mock(TicketCategoryMapper.class);
        when(categoryMapper.findById(1L)).thenReturn(new TicketCategory());
        SlaRuleServiceImpl service = new SlaRuleServiceImpl(mock(SlaRuleMapper.class), categoryMapper,
                mock(AuditLogService.class), mock(SnowflakeIdGenerator.class), new SlaRuleConverter());
        assertThatThrownBy(() -> service.create(request(8, 4), 1L, null, null))
                .isInstanceOf(BusinessException.class).hasMessageContaining("解决时限不能小于响应时限");
    }

    private SlaRuleMutationRequest request(int responseHours, int resolveHours) {
        SlaRuleMutationRequest request = new SlaRuleMutationRequest();
        request.setCategoryId("1"); request.setPriority("HIGH"); request.setResponseHours(responseHours);
        request.setResolveHours(resolveHours); request.setEnabled(true); return request;
    }
}
