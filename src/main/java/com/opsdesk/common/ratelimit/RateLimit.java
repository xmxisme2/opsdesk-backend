package com.opsdesk.common.ratelimit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口限流注解。
 *
 * <p>标注在 Controller 方法上，由统一切面在进入业务逻辑前基于 Redis 计数校验请求频率。</p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(RateLimits.class)
public @interface RateLimit {

    /** 窗口内允许请求次数：由后端注解声明，不允许外部传入。 */
    int limit();

    /** 固定窗口秒数：由后端注解声明，不允许外部传入。 */
    int windowSeconds();

    /** 限流身份维度：决定按 IP、用户或手机号组装 Redis Key。 */
    RateLimitKeyType keyType() default RateLimitKeyType.AUTO;

    /** 手机号字段名：用于从请求 DTO 读取手机号，默认读取 phone。 */
    String phoneField() default "phone";

    /** 命中限流后的提示文案：统一返回给前端展示。 */
    String message() default "操作过于频繁，请稍后再试";
}
