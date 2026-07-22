package com.opsdesk.notification.service.impl;

import com.opsdesk.common.exception.BusinessException;
import com.opsdesk.system.entity.NotificationTemplate;
import com.opsdesk.system.mapper.NotificationTemplateMapper;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** 通知模板渲染测试，覆盖正常替换、停用抑制和缺失变量保护。 */
class NotificationTemplateRenderServiceImplTest {
    @Test void renderShouldReplaceAllVariables() {
        NotificationTemplateMapper mapper = mock(NotificationTemplateMapper.class);
        when(mapper.findByTypeAndChannel("TICKET_ASSIGNED", "IN_APP")).thenReturn(template(1));
        var result = new NotificationTemplateRenderServiceImpl(mapper).render("TICKET_ASSIGNED", Map.of("operatorName", "管理员", "ticketNo", "TK001", "teamName", "基础设施支持组"));
        assertThat(result.orElseThrow().content()).isEqualTo("管理员 将工单 TK001 分派给 基础设施支持组");
    }
    @Test void renderShouldSkipDisabledTemplate() {
        NotificationTemplateMapper mapper = mock(NotificationTemplateMapper.class); when(mapper.findByTypeAndChannel(anyString(), eq("IN_APP"))).thenReturn(template(0));
        assertThat(new NotificationTemplateRenderServiceImpl(mapper).render("TICKET_ASSIGNED", Map.of())).isEmpty();
    }
    @Test void renderShouldRejectMissingVariable() {
        NotificationTemplateMapper mapper = mock(NotificationTemplateMapper.class); when(mapper.findByTypeAndChannel(anyString(), eq("IN_APP"))).thenReturn(template(1));
        assertThatThrownBy(() -> new NotificationTemplateRenderServiceImpl(mapper).render("TICKET_ASSIGNED", Map.of("ticketNo", "TK001"))).isInstanceOf(BusinessException.class);
    }
    private NotificationTemplate template(int enabled) { NotificationTemplate value = new NotificationTemplate(); value.setEnabled(enabled); value.setTitleTemplate("工单 {ticketNo}"); value.setContentTemplate("{operatorName} 将工单 {ticketNo} 分派给 {teamName}"); return value; }
}
