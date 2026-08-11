package com.opsdesk.system.service.impl;

import com.opsdesk.system.dto.SystemConfigSearchRequest;
import com.opsdesk.system.entity.SystemConfig;
import com.opsdesk.system.mapper.SystemConfigMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 系统配置查询测试，覆盖条件规范化与敏感值脱敏。 */
class SystemConfigServiceImplTest {
    @Test
    void searchShouldNormalizeConditionAndMaskSensitiveValue() {
        SystemConfigMapper mapper = mock(SystemConfigMapper.class);
        SystemConfig config = new SystemConfig();
        config.setConfigKey("ai.secret");
        config.setConfigValue("never-return-this");
        config.setConfigGroup("AI");
        config.setDescription("模型密钥");
        config.setEditable(0);
        config.setSensitive(1);
        when(mapper.search("AI", "模型")).thenReturn(List.of(config));

        var result = new SystemConfigServiceImpl(mapper).search(new SystemConfigSearchRequest(" ai ", " 模型 "));

        assertThat(result).singleElement().satisfies(item -> {
            assertThat(item.key()).isEqualTo("ai.secret");
            assertThat(item.value()).isEqualTo("******");
            assertThat(item.editable()).isFalse();
        });
        verify(mapper).search("AI", "模型");
    }
}
