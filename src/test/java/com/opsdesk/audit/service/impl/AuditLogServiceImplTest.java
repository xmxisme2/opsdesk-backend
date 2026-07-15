package com.opsdesk.audit.service.impl;

import com.opsdesk.audit.dto.AuditLogSearchRequest;
import com.opsdesk.audit.entity.AuditLog;
import com.opsdesk.audit.mapper.AuditLogMapper;
import com.opsdesk.common.exception.BusinessException;
import com.opsdesk.common.id.SnowflakeIdGenerator;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 审计日志分页检索服务测试。 */
class AuditLogServiceImplTest {

    @Test
    void shouldNormalizeFiltersAndConvertIdsToStrings() {
        AuditLogMapper mapper = mock(AuditLogMapper.class);
        AuditLog auditLog = new AuditLog();
        auditLog.setId(101L);
        auditLog.setOperatorId(1L);
        auditLog.setOperatorName("系统管理员");
        auditLog.setOperationType("UPDATE");
        auditLog.setBizType("ROLE");
        auditLog.setBizId(2L);
        auditLog.setContent("修改角色权限");
        auditLog.setCreateTime(LocalDateTime.of(2026, 7, 13, 9, 30));
        when(mapper.search(any(), any(), any(), any(), any(), any(), any())).thenReturn(List.of(auditLog));

        AuditLogSearchRequest request = new AuditLogSearchRequest();
        request.setOperatorId("1");
        request.setOperationType(" update ");
        request.setBizType(" role ");
        request.setBizId("2");
        request.setDateFrom("2026-07-01");
        request.setDateTo("2026-07-31");
        request.setKeyword(" 权限 ");

        AuditLogServiceImpl service = new AuditLogServiceImpl(mapper, mock(SnowflakeIdGenerator.class));
        var result = service.search(request);

        assertThat(result.records()).singleElement().satisfies(item -> {
            assertThat(item.id()).isEqualTo("101");
            assertThat(item.operatorId()).isEqualTo("1");
            assertThat(item.bizId()).isEqualTo("2");
            assertThat(item.operatorName()).isEqualTo("系统管理员");
        });
        ArgumentCaptor<LocalDateTime> fromCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> toCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(mapper).search(eq(1L), eq("UPDATE"), eq("ROLE"), eq(2L),
                fromCaptor.capture(), toCaptor.capture(), eq("权限"));
        assertThat(fromCaptor.getValue()).isEqualTo(LocalDateTime.of(2026, 7, 1, 0, 0));
        assertThat(toCaptor.getValue().toLocalDate()).isEqualTo(java.time.LocalDate.of(2026, 7, 31));
    }

    @Test
    void shouldRejectReversedDateRange() {
        AuditLogSearchRequest request = new AuditLogSearchRequest();
        request.setDateFrom("2026-07-31");
        request.setDateTo("2026-07-01");
        AuditLogServiceImpl service = new AuditLogServiceImpl(mock(AuditLogMapper.class), mock(SnowflakeIdGenerator.class));

        assertThatThrownBy(() -> service.search(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("开始时间不能晚于结束时间");
    }

    @Test
    void recordStrictShouldPropagateMapperFailure() {
        AuditLogMapper mapper = mock(AuditLogMapper.class);
        SnowflakeIdGenerator idGenerator = mock(SnowflakeIdGenerator.class);
        when(idGenerator.nextId()).thenReturn(100L);
        when(mapper.insert(any(AuditLog.class))).thenThrow(new IllegalStateException("database unavailable"));
        AuditLogServiceImpl service = new AuditLogServiceImpl(mapper, idGenerator);

        assertThatThrownBy(() -> service.recordStrict(1L, "UPDATE", "SYSTEM_CONFIG", null,
                "更新配置", "127.0.0.1", "JUnit"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("database unavailable");
    }

    @Test
    void recordStrictShouldRejectUnexpectedAffectedRows() {
        AuditLogMapper mapper = mock(AuditLogMapper.class);
        SnowflakeIdGenerator idGenerator = mock(SnowflakeIdGenerator.class);
        when(idGenerator.nextId()).thenReturn(100L);
        when(mapper.insert(any(AuditLog.class))).thenReturn(0);
        AuditLogServiceImpl service = new AuditLogServiceImpl(mapper, idGenerator);

        assertThatThrownBy(() -> service.recordStrict(1L, "UPDATE", "SYSTEM_CONFIG", null,
                "更新配置", "127.0.0.1", "JUnit"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("审计日志写入失败");
    }

    @Test
    void recordShouldKeepBestEffortSemanticsWhenMapperFails() {
        AuditLogMapper mapper = mock(AuditLogMapper.class);
        when(mapper.insert(any(AuditLog.class))).thenThrow(new IllegalStateException("database unavailable"));
        AuditLogServiceImpl service = new AuditLogServiceImpl(mapper, mock(SnowflakeIdGenerator.class));

        service.record(1L, "LOGIN", "USER", 1L, "登录", "127.0.0.1", "JUnit");

        verify(mapper).insert(any(AuditLog.class));
    }
}
