package com.opsdesk.system.service.impl;

import com.opsdesk.audit.service.AuditLogService;
import com.opsdesk.common.exception.BusinessException;
import com.opsdesk.system.dto.NotificationTemplateUpdateRequest;
import com.opsdesk.system.entity.NotificationTemplate;
import com.opsdesk.system.mapper.NotificationTemplateMapper;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** 通知模板服务测试，覆盖查询、合法变量更新和未知变量拒绝。 */
class NotificationTemplateServiceImplTest {
    @Test void searchShouldReturnAllowedVariables() {
        NotificationTemplateMapper mapper = mock(NotificationTemplateMapper.class);
        when(mapper.search(null, "IN_APP")).thenReturn(List.of(template()));
        var result = new NotificationTemplateServiceImpl(mapper, mock(AuditLogService.class)).search(new com.opsdesk.system.dto.NotificationTemplateSearchRequest(null, "in_app"));
        assertThat(result.get(0).allowedVariables()).contains("ticketNo", "assignee", "operatorName");
        assertThat(result.get(0).variableDescriptions()).containsEntry("operatorName", "执行本次分派或状态变更的操作人姓名");
    }
    @Test void updateShouldValidateAndAudit() {
        NotificationTemplateMapper mapper = mock(NotificationTemplateMapper.class); AuditLogService audit = mock(AuditLogService.class);
        NotificationTemplate template = template(); when(mapper.findById(1L)).thenReturn(template); when(mapper.update(template)).thenReturn(1);
        var result = new NotificationTemplateServiceImpl(mapper, audit).update("1", new NotificationTemplateUpdateRequest("已分派 {ticketNo}", "处理人 {assignee}", true), 9L, "ip", "ua");
        assertThat(result.enabled()).isTrue(); verify(audit).record(eq(9L), eq("UPDATE"), eq("SYSTEM_CONFIG"), eq(1L), anyString(), eq("ip"), eq("ua"));
    }
    @Test void updateShouldRejectUnknownVariable() {
        NotificationTemplateMapper mapper = mock(NotificationTemplateMapper.class); when(mapper.findById(1L)).thenReturn(template());
        var service = new NotificationTemplateServiceImpl(mapper, mock(AuditLogService.class));
        assertThatThrownBy(() -> service.update("1", new NotificationTemplateUpdateRequest("{unknown}", "正文", true), 9L, "", "")).isInstanceOf(BusinessException.class);
    }
    @Test void updateShouldAllowAssignmentOperatorVariable() {
        NotificationTemplateMapper mapper = mock(NotificationTemplateMapper.class); NotificationTemplate template = template();
        when(mapper.findById(1L)).thenReturn(template); when(mapper.update(template)).thenReturn(1);
        var service = new NotificationTemplateServiceImpl(mapper, mock(AuditLogService.class));
        assertThatCode(() -> service.update("1", new NotificationTemplateUpdateRequest("{operatorName} 已分派工单", "工单 {ticketNo} 已分派给 {assignee}", true), 9L, "", "")).doesNotThrowAnyException();
    }
    private NotificationTemplate template() { NotificationTemplate value = new NotificationTemplate(); value.setId(1L); value.setType("TICKET_ASSIGNED"); value.setChannel("IN_APP"); value.setTitleTemplate("工单已分派"); value.setContentTemplate("工单 {ticketNo} 已分派给 {assignee}"); value.setEnabled(1); return value; }
}
