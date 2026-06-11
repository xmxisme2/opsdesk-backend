package com.opsdesk.common.ratelimit;

import com.opsdesk.common.security.CurrentUser;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.HandlerMapping;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
 * 接口限流切面。
 *
 * <p>读取 Controller 方法上的 RateLimit 注解，按接口路径、身份维度和时间窗口生成 Redis Key，并在业务逻辑执行前完成限流校验。</p>
 */
@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class RateLimitAspect {

    /** Redis Key 前缀：所有接口限流计数统一写入该命名空间，便于排查和清理。 */
    private static final String RATE_LIMIT_KEY_PREFIX = "rate_limit:";

    /** 匿名用户标识：未登录且无法提取用户 ID 时使用，不允许外部传入。 */
    private static final String ANONYMOUS_IDENTITY = "anonymous";

    /** 未提供手机号标识：公开接口缺少手机号字段时参与限流 Key，避免空值造成异常。 */
    private static final String MISSING_PHONE_IDENTITY = "missing_phone";

    /** 代理转发 IP 请求头：部署在网关或反向代理后优先读取第一个客户端 IP。 */
    private static final String HEADER_X_FORWARDED_FOR = "X-Forwarded-For";

    /** 真实客户端 IP 请求头：部分代理会使用该头传递原始 IP。 */
    private static final String HEADER_X_REAL_IP = "X-Real-IP";

    private final RedisRateLimitService rateLimitService;

    public RateLimitAspect(RedisRateLimitService rateLimitService) {
        this.rateLimitService = rateLimitService;
    }

    @Around("@annotation(com.opsdesk.common.ratelimit.RateLimit) || @annotation(com.opsdesk.common.ratelimit.RateLimits)")
    public Object checkRateLimit(ProceedingJoinPoint joinPoint) throws Throwable {
        HttpServletRequest request = currentRequest();
        if (request == null) {
            return joinPoint.proceed();
        }

        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        Set<RateLimit> rules = AnnotatedElementUtils.findMergedRepeatableAnnotations(
                method,
                RateLimit.class,
                RateLimits.class
        );
        for (RateLimit rule : rules) {
            String key = buildRedisKey(rule, joinPoint.getArgs(), request);
            rateLimitService.check(key, rule.limit(), rule.windowSeconds(), rule.message());
        }
        return joinPoint.proceed();
    }

    private String buildRedisKey(RateLimit rule, Object[] args, HttpServletRequest request) {
        String path = requestPath(request);
        String identity = resolveIdentity(rule, args, request);
        String rawKey = path + ':' + rule.keyType() + ':' + identity + ':' + rule.limit() + ':' + rule.windowSeconds();
        return RATE_LIMIT_KEY_PREFIX + DigestUtils.md5DigestAsHex(rawKey.getBytes(StandardCharsets.UTF_8));
    }

    private String resolveIdentity(RateLimit rule, Object[] args, HttpServletRequest request) {
        String clientIp = clientIp(request);
        Long currentUserId = currentUserId();
        return switch (rule.keyType()) {
            case IP -> "ip:" + clientIp;
            case USER -> currentUserId == null ? ANONYMOUS_IDENTITY + ':' + clientIp : "user:" + currentUserId;
            case PHONE -> "phone:" + phoneValue(args, rule.phoneField());
            case IP_AND_PHONE -> "ip_phone:" + clientIp + ':' + phoneValue(args, rule.phoneField());
            case AUTO -> currentUserId == null ? "ip:" + clientIp : "user:" + currentUserId;
        };
    }

    private String phoneValue(Object[] args, String phoneField) {
        if (args == null) {
            return MISSING_PHONE_IDENTITY;
        }
        for (Object arg : args) {
            if (arg == null) {
                continue;
            }
            BeanWrapper beanWrapper = PropertyAccessorFactory.forBeanPropertyAccess(arg);
            if (!beanWrapper.isReadableProperty(phoneField)) {
                continue;
            }
            Object value = beanWrapper.getPropertyValue(phoneField);
            if (value != null && StringUtils.hasText(String.valueOf(value))) {
                return String.valueOf(value).trim();
            }
        }
        return MISSING_PHONE_IDENTITY;
    }

    private Long currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CurrentUser currentUser)) {
            return null;
        }
        return currentUser.getUserId();
    }

    private String requestPath(HttpServletRequest request) {
        Object bestMatchingPattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        if (bestMatchingPattern != null && StringUtils.hasText(String.valueOf(bestMatchingPattern))) {
            return String.valueOf(bestMatchingPattern);
        }
        return request.getRequestURI();
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader(HEADER_X_FORWARDED_FOR);
        if (StringUtils.hasText(forwardedFor)) {
            return forwardedFor.split(",")[0].trim();
        }
        String realIp = request.getHeader(HEADER_X_REAL_IP);
        if (StringUtils.hasText(realIp)) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }

    private HttpServletRequest currentRequest() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servletRequestAttributes) {
            return servletRequestAttributes.getRequest();
        }
        return null;
    }
}
