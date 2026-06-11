package com.opsdesk.common.ratelimit;

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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 限流切面测试。
 *
 * <p>验证 Controller 注解能被切面读取，并在执行业务方法前调用 Redis 限流服务。</p>
 */
@ExtendWith(MockitoExtension.class)
class RateLimitAspectTest {

    @Mock
    private RedisRateLimitService rateLimitService;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private MethodSignature methodSignature;

    private RateLimitAspect rateLimitAspect;

    @BeforeEach
    void setUp() {
        rateLimitAspect = new RateLimitAspect(rateLimitService);
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
        SecurityContextHolder.clearContext();
    }

    @Test
    void checkRateLimitShouldCallRedisServiceBeforeProceed() throws Throwable {
        Method method = DemoController.class.getDeclaredMethod("search");
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(method);
        when(joinPoint.getArgs()).thenReturn(new Object[0]);
        when(joinPoint.proceed()).thenReturn("ok");

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/users/search");
        request.setRemoteAddr("10.0.0.1");
        request.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/api/users/search");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(new UsernamePasswordAuthenticationToken(
                new CurrentUser(1L, "13800000000", "admin", List.of("ADMIN"), List.of()),
                null,
                List.of()
        ));
        SecurityContextHolder.setContext(securityContext);

        Object result = rateLimitAspect.checkRateLimit(joinPoint);

        assertThat(result).isEqualTo("ok");
        verify(rateLimitService).check(
                startsWith("rate_limit:"),
                eq(RateLimitDefaults.SEARCH_LIMIT_PER_MINUTE),
                eq((long) RateLimitDefaults.ONE_MINUTE_SECONDS),
                eq("操作过于频繁，请稍后再试")
        );
        verify(joinPoint).proceed();
    }

    private static class DemoController {

        @RateLimit(limit = RateLimitDefaults.SEARCH_LIMIT_PER_MINUTE,
                windowSeconds = RateLimitDefaults.ONE_MINUTE_SECONDS,
                keyType = RateLimitKeyType.USER)
        String search() {
            return "ok";
        }
    }
}
