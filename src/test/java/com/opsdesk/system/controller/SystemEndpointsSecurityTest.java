package com.opsdesk.system.controller;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import static org.assertj.core.api.Assertions.assertThat;

/** 三个补齐接口的权限入口回归测试。 */
class SystemEndpointsSecurityTest {
    @Test
    void systemConfigAndAiSettingsShouldRequireAdminRole() {
        assertThat(SystemConfigController.class.getAnnotation(PreAuthorize.class).value()).isEqualTo("hasRole('ADMIN')");
        assertThat(AiSettingsController.class.getAnnotation(PreAuthorize.class).value()).isEqualTo("hasRole('ADMIN')");
    }
}
