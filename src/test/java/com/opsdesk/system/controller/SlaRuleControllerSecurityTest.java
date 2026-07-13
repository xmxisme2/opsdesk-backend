package com.opsdesk.system.controller;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import static org.assertj.core.api.Assertions.assertThat;

/** SLA 管理 Controller 权限回归测试。 */
class SlaRuleControllerSecurityTest {
    @Test
    void controllerShouldRequireAdminRole() {
        PreAuthorize annotation = SlaRuleController.class.getAnnotation(PreAuthorize.class);
        assertThat(annotation).isNotNull();
        assertThat(annotation.value()).isEqualTo("hasRole('ADMIN')");
    }
}
