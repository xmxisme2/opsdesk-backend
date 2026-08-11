package com.opsdesk.ai.controller;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * AI 管理接口权限注解测试。
 */
class AiAdminControllerSecurityTest {

    @Test
    void healthEndpointShouldRequireAdminRole() throws Exception {
        Method method = AiAdminController.class.getMethod(
                "health",
                com.opsdesk.common.security.CurrentUser.class
        );

        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertEquals("hasRole('ADMIN')", preAuthorize.value());
    }

    @Test
    void modelTestEndpointShouldRequireAdminRole() throws Exception {
        Method method = AiAdminController.class.getMethod(
                "testConnection",
                com.opsdesk.ai.dto.AiConnectionTestRequest.class,
                com.opsdesk.common.security.CurrentUser.class
        );

        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertEquals("hasRole('ADMIN')", preAuthorize.value());
    }

    @Test
    void qualityOverviewEndpointShouldRequireAdminRole() throws Exception {
        Method method = AiAdminController.class.getMethod(
                "qualityOverview",
                com.opsdesk.ai.dto.AiQualityRangeRequest.class,
                com.opsdesk.common.security.CurrentUser.class
        );

        assertEquals("hasRole('ADMIN')", method.getAnnotation(PreAuthorize.class).value());
    }

    @Test
    void qualitySamplesEndpointShouldRequireAdminRole() throws Exception {
        Method method = AiAdminController.class.getMethod(
                "searchQualitySamples",
                com.opsdesk.ai.dto.AiQualitySampleSearchRequest.class,
                com.opsdesk.common.security.CurrentUser.class
        );

        assertEquals("hasRole('ADMIN')", method.getAnnotation(PreAuthorize.class).value());
    }
}
