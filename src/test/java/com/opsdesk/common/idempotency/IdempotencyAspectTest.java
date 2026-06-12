package com.opsdesk.common.idempotency;

import com.opsdesk.common.response.ApiResponse;
import com.opsdesk.common.security.CurrentUser;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.HandlerMapping;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 幂等切面测试。
 *
 * <p>验证 Controller 标记幂等注解后，只有携带 Idempotency-Key 的请求才进入 Redis 幂等服务。</p>
 */
@ExtendWith(MockitoExtension.class)
class IdempotencyAspectTest {

    @Mock
    private RedisIdempotencyService idempotencyService;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private MethodSignature methodSignature;

    private IdempotencyAspect idempotencyAspect;

    @BeforeEach
    void setUp() {
        idempotencyAspect = new IdempotencyAspect(idempotencyService);
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
        SecurityContextHolder.clearContext();
    }

    @Test
    void applyIdempotencyShouldUseRedisServiceWhenHeaderExists() throws Throwable {
        Method method = DemoController.class.getDeclaredMethod("resetPassword");
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(method);
        when(idempotencyService.execute(startsWith("idempotency:"), eq(600L), eq(30L), any()))
                .thenReturn(ApiResponse.success("cached"));
        bindRequest("same-key");
        bindCurrentUser();

        Object result = idempotencyAspect.applyIdempotency(joinPoint);

        assertThat(result).isInstanceOf(ApiResponse.class);
        verify(idempotencyService).execute(startsWith("idempotency:"), eq(600L), eq(30L), any());
        verify(joinPoint, never()).proceed();
    }

    @Test
    void applyIdempotencyShouldBypassRedisServiceWhenHeaderMissing() throws Throwable {
        when(joinPoint.proceed()).thenReturn(ApiResponse.success("fresh"));
        bindRequest(null);
        bindCurrentUser();

        Object result = idempotencyAspect.applyIdempotency(joinPoint);

        assertThat(result).isInstanceOf(ApiResponse.class);
        verify(idempotencyService, never()).execute(any(), any(Long.class), any(Long.class), any());
        verify(joinPoint).proceed();
    }

    private void bindRequest(String idempotencyKey) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/users/2/reset-password");
        request.setRemoteAddr("10.0.0.1");
        request.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/api/users/{id}/reset-password");
        if (idempotencyKey != null) {
            request.addHeader("Idempotency-Key", idempotencyKey);
        }
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    private void bindCurrentUser() {
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(new UsernamePasswordAuthenticationToken(
                new CurrentUser(1L, "13800000000", "admin", List.of("ADMIN"), List.of()),
                null,
                List.of()
        ));
        SecurityContextHolder.setContext(securityContext);
    }

    private static class DemoController {

        @Idempotent
        ApiResponse<String> resetPassword() {
            return ApiResponse.success("fresh");
        }
    }
}
