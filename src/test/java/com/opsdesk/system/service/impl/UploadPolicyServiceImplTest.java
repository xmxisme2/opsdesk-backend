package com.opsdesk.system.service.impl;

import com.opsdesk.audit.service.AuditLogService;
import com.opsdesk.common.exception.BusinessException;
import com.opsdesk.system.dto.UploadPolicyUpdateRequest;
import com.opsdesk.system.entity.SystemConfig;
import com.opsdesk.system.mapper.SystemConfigMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** 上传限制服务测试，覆盖默认读取、分类完整性和安全预览边界。 */
class UploadPolicyServiceImplTest {
    @Test
    void detailShouldUseStoredValuesAndDefaults() {
        SystemConfigMapper mapper = mock(SystemConfigMapper.class);
        when(mapper.findByGroup("UPLOAD")).thenReturn(List.of(config("upload.max_file_size_mb", "40")));
        var result = new UploadPolicyServiceImpl(mapper, mock(AuditLogService.class)).detail();
        assertThat(result.maxFileSizeMb()).isEqualTo(40);
        assertThat(result.maxFilesPerTicket()).isEqualTo(10);
        assertThat(result.allowedExtensions()).contains("png", "zip");
    }

    @Test
    void updateShouldPersistFiveKeysAndAudit() {
        SystemConfigMapper mapper = mock(SystemConfigMapper.class);
        AuditLogService audit = mock(AuditLogService.class);
        when(mapper.updateValue(anyString(), anyString(), eq(1L))).thenReturn(1);
        var service = new UploadPolicyServiceImpl(mapper, audit);
        var result = service.update(new UploadPolicyUpdateRequest(40, 20, List.of("png", "pdf"), List.of("png"), List.of("pdf")), 1L, "127.0.0.1", "JUnit");
        assertThat(result.maxFileSizeMb()).isEqualTo(40);
        verify(mapper, times(5)).updateValue(anyString(), anyString(), eq(1L));
        verify(audit).record(eq(1L), eq("UPDATE"), eq("SYSTEM_CONFIG"), isNull(), anyString(), anyString(), anyString());
    }

    @Test
    void updateShouldRejectUnsafePreviewExtension() {
        var service = new UploadPolicyServiceImpl(mock(SystemConfigMapper.class), mock(AuditLogService.class));
        assertThatThrownBy(() -> service.update(new UploadPolicyUpdateRequest(20, 10, List.of("pdf"), List.of("pdf"), List.of()), 1L, "", ""))
                .isInstanceOf(BusinessException.class);
    }

    private SystemConfig config(String key, String value) {
        SystemConfig config = new SystemConfig(); config.setConfigKey(key); config.setConfigValue(value); return config;
    }
}
