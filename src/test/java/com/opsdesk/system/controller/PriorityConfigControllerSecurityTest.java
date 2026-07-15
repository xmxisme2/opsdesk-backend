package com.opsdesk.system.controller;

import com.opsdesk.common.idempotency.Idempotent;
import com.opsdesk.common.ratelimit.RateLimit;
import com.opsdesk.common.ratelimit.RateLimitDefaults;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

/** 优先级配置 Controller 安全注解测试，防止读取与更新权限回退。 */
class PriorityConfigControllerSecurityTest {
    @Test
    void optionsShouldRequireAuthenticationAndSearchRateLimit() {
        Method method = method("options");

        assertThat(method.getAnnotation(PreAuthorize.class).value()).isEqualTo("isAuthenticated()");
        assertThat(method.getAnnotation(RateLimit.class).limit()).isEqualTo(RateLimitDefaults.SEARCH_LIMIT_PER_MINUTE);
    }

    @Test
    void updateShouldRequireAdminIdempotencyAndActionRateLimit() {
        Method method = method("update");

        assertThat(method.getAnnotation(PreAuthorize.class).value()).isEqualTo("hasRole('ADMIN')");
        assertThat(method.getAnnotation(Idempotent.class)).isNotNull();
        assertThat(method.getAnnotation(RateLimit.class).limit()).isEqualTo(RateLimitDefaults.ACTION_LIMIT_PER_MINUTE);
    }

    private Method method(String name) {
        return java.util.Arrays.stream(PriorityConfigController.class.getDeclaredMethods())
                .filter(candidate -> candidate.getName().equals(name))
                .findFirst()
                .orElseThrow();
    }
}
