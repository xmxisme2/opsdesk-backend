package com.opsdesk.ticket.controller;

import com.opsdesk.common.idempotency.Idempotent;
import com.opsdesk.common.ratelimit.RateLimit;
import com.opsdesk.common.ratelimit.RateLimitDefaults;
import com.opsdesk.common.ratelimit.RateLimitKeyType;
import com.opsdesk.common.security.CurrentUser;
import com.opsdesk.ticket.dto.TicketCategoryMutationRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

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
}
