package com.opsdesk.system.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opsdesk.audit.service.AuditLogService;
import com.opsdesk.common.exception.BusinessException;
import com.opsdesk.common.exception.ErrorCode;
import com.opsdesk.system.dto.PriorityConfigUpdateRequest;
import com.opsdesk.system.entity.SystemConfig;
import com.opsdesk.system.mapper.SystemConfigMapper;
import com.opsdesk.system.service.impl.PriorityConfigServiceImpl;
import com.opsdesk.system.vo.PriorityOptionVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** 优先级配置服务测试，覆盖默认回退、固定编码校验和整体更新。 */
class PriorityConfigServiceImplTest {
    private SystemConfigMapper mapper;
    private AuditLogService auditLogService;
    private PriorityConfigServiceImpl service;

    @BeforeEach
    void setUp() {
        mapper = mock(SystemConfigMapper.class);
        auditLogService = mock(AuditLogService.class);
        service = new PriorityConfigServiceImpl(mapper, auditLogService, new ObjectMapper());
    }

    @Test
    void optionsShouldMergeMissingRowsWithDefaults() {
        when(mapper.findByGroup("PRIORITY")).thenReturn(List.of(
                config("priority.LOW", "{\"name\":\"较低\",\"sort\":5,\"color\":\"#aabbcc\",\"enabled\":true}")));

        List<PriorityOptionVO> result = service.options();

        assertThat(result).extracting(PriorityOptionVO::code)
                .containsExactly("LOW", "MEDIUM", "HIGH", "URGENT");
        assertThat(result.get(0)).isEqualTo(new PriorityOptionVO("LOW", "较低", 5, "#AABBCC", true));
        assertThat(result.get(1)).isEqualTo(new PriorityOptionVO("MEDIUM", "中", 20, "#1252AD", true));
    }

    @Test
    void optionsShouldFallbackOnlyBrokenItems() {
        when(mapper.findByGroup("PRIORITY")).thenReturn(List.of(
                config("priority.LOW", "not-json"),
                config("priority.MEDIUM", "{\"name\":\"普通\",\"sort\":25,\"color\":\"#123abc\",\"enabled\":true}")));

        List<PriorityOptionVO> result = service.options();

        assertThat(result.get(0)).isEqualTo(new PriorityOptionVO("LOW", "低", 10, "#0D8052", true));
        assertThat(result.get(1)).isEqualTo(new PriorityOptionVO("MEDIUM", "普通", 25, "#123ABC", true));
    }

    @Test
    void optionsShouldOrderItemsByConfiguredSort() {
        when(mapper.findByGroup("PRIORITY")).thenReturn(List.of(
                config("priority.URGENT", "{\"name\":\"紧急\",\"sort\":1,\"color\":\"#C71F24\",\"enabled\":true}")));

        List<PriorityOptionVO> result = service.options();

        assertThat(result).extracting(PriorityOptionVO::code)
                .containsExactly("URGENT", "LOW", "MEDIUM", "HIGH");
    }

    @Test
    void optionsShouldFallbackDisabledMediumAndDuplicateSort() {
        when(mapper.findByGroup("PRIORITY")).thenReturn(List.of(
                config("priority.LOW", "{\"name\":\"较低\",\"sort\":20,\"color\":\"#0D8052\",\"enabled\":true}"),
                config("priority.MEDIUM", "{\"name\":\"普通\",\"sort\":25,\"color\":\"#1252AD\",\"enabled\":false}")));

        List<PriorityOptionVO> result = service.options();

        assertThat(result).contains(new PriorityOptionVO("LOW", "低", 10, "#0D8052", true));
        assertThat(result).contains(new PriorityOptionVO("MEDIUM", "中", 20, "#1252AD", true));
        assertThat(result).extracting(PriorityOptionVO::sort).doesNotHaveDuplicates();
    }

    @Test
    void updateShouldRejectDisabledMedium() {
        PriorityConfigUpdateRequest request = requestWith(new PriorityOptionVO("MEDIUM", "中", 20, "#1252AD", false));

        assertThatThrownBy(() -> service.update(request, 1L, "127.0.0.1", "JUnit"))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.PARAM_ERROR));
    }

    @Test
    void updateShouldRejectNonFixedCodeSetAndDuplicateSort() {
        List<PriorityOptionVO> items = defaultItems();
        items.set(3, new PriorityOptionVO("CUSTOM", "自定义", 30, "#C71F24", true));

        assertThatThrownBy(() -> service.update(new PriorityConfigUpdateRequest(items), 1L, "", ""))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void updateShouldNormalizeColorsPersistFourItemsAndAuditOnce() {
        when(mapper.updateValue(anyString(), anyString(), eq(9L))).thenReturn(1);
        List<PriorityOptionVO> items = defaultItems();
        items.set(0, new PriorityOptionVO("LOW", "低", 10, "#abcdef", true));

        List<PriorityOptionVO> result = service.update(new PriorityConfigUpdateRequest(items), 9L, "127.0.0.1", "JUnit");

        assertThat(result.get(0).color()).isEqualTo("#ABCDEF");
        verify(mapper, times(4)).updateValue(startsWith("priority."), anyString(), eq(9L));
        verify(auditLogService, times(1)).record(9L, "UPDATE", "SYSTEM_CONFIG", null,
                "更新工单优先级配置", "127.0.0.1", "JUnit");
    }

    private PriorityConfigUpdateRequest requestWith(PriorityOptionVO replacement) {
        List<PriorityOptionVO> items = defaultItems();
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).code().equals(replacement.code())) {
                items.set(i, replacement);
            }
        }
        return new PriorityConfigUpdateRequest(items);
    }

    private List<PriorityOptionVO> defaultItems() {
        return new java.util.ArrayList<>(List.of(
                new PriorityOptionVO("LOW", "低", 10, "#0D8052", true),
                new PriorityOptionVO("MEDIUM", "中", 20, "#1252AD", true),
                new PriorityOptionVO("HIGH", "高", 30, "#BA630F", true),
                new PriorityOptionVO("URGENT", "紧急", 40, "#C71F24", true)));
    }

    private SystemConfig config(String key, String value) {
        SystemConfig config = new SystemConfig();
        config.setConfigKey(key);
        config.setConfigValue(value);
        return config;
    }
}
