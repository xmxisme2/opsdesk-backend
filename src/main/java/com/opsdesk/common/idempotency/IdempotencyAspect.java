package com.opsdesk.common.idempotency;

import com.opsdesk.common.exception.BusinessException;
import com.opsdesk.common.exception.ErrorCode;
import com.opsdesk.common.security.CurrentUser;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
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

/**
 * 关键写操作幂等切面。
 *
 * <p>读取 Idempotent 注解和 Idempotency-Key 请求头，按用户、接口路径和请求头生成 Redis Key，再交由 Redis 幂等服务保护业务方法。</p>
 */
@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 30)
public class IdempotencyAspect {

    /** 幂等请求头：前端关键写操作传入，同一个动作重试时必须保持不变。 */
    public static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";

    /** Redis Key 前缀：所有幂等请求统一写入该命名空间，便于排查和清理。 */
    private static final String IDEMPOTENCY_KEY_PREFIX = "idempotency:";

    /** 幂等请求头最大长度：防止异常长 Header 造成 Redis Key 构造和日志排查问题。 */
    private static final int MAX_IDEMPOTENCY_KEY_LENGTH = 128;

    /** 匿名用户标识：公开接口无登录态时结合 IP 参与 Key 生成。 */
    private static final String ANONYMOUS_IDENTITY = "anonymous";

    private final RedisIdempotencyService idempotencyService;

    public IdempotencyAspect(RedisIdempotencyService idempotencyService) {
        this.idempotencyService = idempotencyService;
    }

    @Around("@annotation(com.opsdesk.common.idempotency.Idempotent)")
    public Object applyIdempotency(ProceedingJoinPoint joinPoint) throws Throwable {
        HttpServletRequest request = currentRequest();
        if (request == null) {
            return joinPoint.proceed();
        }

        String idempotencyKey = request.getHeader(IDEMPOTENCY_KEY_HEADER);
        if (!StringUtils.hasText(idempotencyKey)) {
            return joinPoint.proceed();
        }
        String normalizedKey = idempotencyKey.trim();
        if (normalizedKey.length() > MAX_IDEMPOTENCY_KEY_LENGTH) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "Idempotency-Key 长度不能超过 128 个字符");
        }

        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        Idempotent annotation = AnnotatedElementUtils.findMergedAnnotation(method, Idempotent.class);
        if (annotation == null) {
            return joinPoint.proceed();
        }

        String redisKey = buildRedisKey(request, normalizedKey);
        return idempotencyService.execute(
                redisKey,
                annotation.resultTtlSeconds(),
                annotation.lockTtlSeconds(),
                joinPoint::proceed
        );
    }

    private String buildRedisKey(HttpServletRequest request, String idempotencyKey) {
        String rawKey = currentIdentity(request) + ':' + request.getMethod() + ':' + requestPath(request) + ':' + idempotencyKey;
        return IDEMPOTENCY_KEY_PREFIX + DigestUtils.md5DigestAsHex(rawKey.getBytes(StandardCharsets.UTF_8));
    }

    private String currentIdentity(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CurrentUser currentUser) {
            return "user:" + currentUser.getUserId();
        }
        return ANONYMOUS_IDENTITY + ':' + request.getRemoteAddr();
    }

    private String requestPath(HttpServletRequest request) {
        Object bestMatchingPattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        if (bestMatchingPattern != null && StringUtils.hasText(String.valueOf(bestMatchingPattern))) {
            return String.valueOf(bestMatchingPattern);
        }
        return request.getRequestURI();
    }

    private HttpServletRequest currentRequest() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servletRequestAttributes) {
            return servletRequestAttributes.getRequest();
        }
        return null;
    }
}
