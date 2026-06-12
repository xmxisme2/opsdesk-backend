package com.opsdesk.common.idempotency;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 关键写操作幂等注解。
 *
 * <p>标记在 Controller 方法上，当前仅在请求携带 Idempotency-Key 时生效；未携带时保持原业务行为，避免破坏旧调用方。</p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Idempotent {

    /** 结果缓存时间：重复请求在该时间内直接复用第一次成功响应，不允许外部传入。 */
    long resultTtlSeconds() default 600;

    /** 执行锁时间：首个请求尚未完成时阻止同 Key 重复进入业务方法，不允许外部传入。 */
    long lockTtlSeconds() default 30;
}
