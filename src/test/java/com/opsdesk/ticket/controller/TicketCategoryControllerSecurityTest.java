package com.opsdesk.ticket.controller;

import com.opsdesk.common.idempotency.Idempotent;
import com.opsdesk.common.ratelimit.RateLimit;
import com.opsdesk.common.ratelimit.RateLimitDefaults;
import com.opsdesk.common.ratelimit.RateLimitKeyType;
import com.opsdesk.common.security.CurrentUser;
import com.opsdesk.ticket.dto.TicketCategoryMutationRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/** 工单分类写接口权限、幂等和用户维度限流注解回归测试。 */
class TicketCategoryControllerSecurityTest {

    @Test
    void writeEndpointsShouldRequireAdminAndActionGuards() throws NoSuchMethodException {
        List<Method> methods = List.of(
                TicketCategoryController.class.getDeclaredMethod(
                        "create", TicketCategoryMutationRequest.class, CurrentUser.class, HttpServletRequest.class),
                TicketCategoryController.class.getDeclaredMethod(
                        "update", String.class, TicketCategoryMutationRequest.class, CurrentUser.class, HttpServletRequest.class),
                TicketCategoryController.class.getDeclaredMethod(
                        "delete", String.class, CurrentUser.class, HttpServletRequest.class)
        );

        for (Method method : methods) {
            PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
            RateLimit rateLimit = method.getAnnotation(RateLimit.class);
            assertThat(preAuthorize).isNotNull();
            assertThat(preAuthorize.value()).isEqualTo("hasRole('ADMIN')");
            assertThat(method.getAnnotation(Idempotent.class)).isNotNull();
            assertThat(rateLimit).isNotNull();
            assertThat(rateLimit.limit()).isEqualTo(RateLimitDefaults.ACTION_LIMIT_PER_MINUTE);
            assertThat(rateLimit.windowSeconds()).isEqualTo(RateLimitDefaults.ONE_MINUTE_SECONDS);
            assertThat(rateLimit.keyType()).isEqualTo(RateLimitKeyType.USER);
        }
    }

    @Test
    void writeEndpointsShouldExposeContractPaths() throws NoSuchMethodException {
        assertThat(postPath("create", TicketCategoryMutationRequest.class, CurrentUser.class, HttpServletRequest.class))
                .isEqualTo("/create");
        assertThat(postPath("update", String.class, TicketCategoryMutationRequest.class,
                CurrentUser.class, HttpServletRequest.class)).isEqualTo("/{id}/update");
        assertThat(postPath("delete", String.class, CurrentUser.class, HttpServletRequest.class))
                .isEqualTo("/{id}/delete");
    }

    @Test
    void writeEndpointsShouldPassOperatorIpAndUserAgentToService() {
        var service = mock(com.opsdesk.ticket.service.TicketCategoryService.class);
        var controller = new TicketCategoryController(service);
        var currentUser = new CurrentUser(9L, "13800000000", "admin", List.of("ADMIN"), List.of());
        var servletRequest = new MockHttpServletRequest();
        servletRequest.setRemoteAddr("10.0.0.8");
        servletRequest.addHeader("User-Agent", "MockMvc-Agent");

        TicketCategoryMutationRequest mutationRequest = new TicketCategoryMutationRequest();
        controller.create(mutationRequest, currentUser, servletRequest);
        controller.update("2", mutationRequest, currentUser, servletRequest);
        controller.delete("2", currentUser, servletRequest);

        verify(service).create(any(TicketCategoryMutationRequest.class),
                eq(9L), eq("10.0.0.8"), eq("MockMvc-Agent"));
        verify(service).update(eq("2"), any(TicketCategoryMutationRequest.class),
                eq(9L), eq("10.0.0.8"), eq("MockMvc-Agent"));
        verify(service).delete(eq("2"), eq(9L), eq("10.0.0.8"), eq("MockMvc-Agent"));
    }

    private String postPath(String methodName, Class<?>... parameterTypes) throws NoSuchMethodException {
        return TicketCategoryController.class.getDeclaredMethod(methodName, parameterTypes)
                .getAnnotation(PostMapping.class).value()[0];
    }
}
