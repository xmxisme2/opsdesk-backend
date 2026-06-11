package com.opsdesk.common.ratelimit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 多重限流注解容器。
 *
 * <p>用于短信验证码等同时需要分钟上限和日上限的接口，业务代码通常无需直接引用本注解。</p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimits {

    /** 多条限流规则：同一个接口命中任意一条规则都会被拦截。 */
    RateLimit[] value();
}
