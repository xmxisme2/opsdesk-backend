package com.opsdesk.audit.controller;

import com.opsdesk.audit.dto.AuditLogSearchRequest;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

/** 审计日志管理接口权限注解回归测试。 */
class AuditLogControllerSecurityTest {

    @Test
    void searchShouldRequireAdminRole() throws NoSuchMethodException {
        Method method = AuditLogController.class.getDeclaredMethod("search", AuditLogSearchRequest.class);
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertThat(preAuthorize).isNotNull();
        assertThat(preAuthorize.value()).isEqualTo("hasRole('ADMIN')");
    }
}
