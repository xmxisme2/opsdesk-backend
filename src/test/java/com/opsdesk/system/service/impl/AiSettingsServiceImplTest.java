package com.opsdesk.system.service.impl;

import com.opsdesk.ai.service.AiHealthProxyService;
import com.opsdesk.ai.vo.AiServiceHealthVO;
import com.opsdesk.common.security.CurrentUser;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** AI 设置查询测试，确保只组合公开信息和独立服务实时开关。 */
class AiSettingsServiceImplTest {
    @Test
    void detailShouldExposeHealthStateWithoutCredentials() {
        AiHealthProxyService healthService = mock(AiHealthProxyService.class);
        CurrentUser user = new CurrentUser(1L, "13800000000", "admin", List.of("ADMIN"), List.of());
        when(healthService.check(user)).thenReturn(new AiServiceHealthVO(
                "UP", "opsdesk-ai-service", true, true, "UP", "UP", true, OffsetDateTime.now()));

        var result = new AiSettingsServiceImpl(healthService).detail(user);

        assertThat(result.enabled()).isTrue();
        assertThat(result.ragEnabled()).isTrue();
        assertThat(result.provider()).isEqualTo("DeepSeek");
        assertThat(result.model()).isEqualTo("deepseek-v4-flash");
        assertThat(result.toString()).doesNotContain("secret", "apiKey");
    }
}
