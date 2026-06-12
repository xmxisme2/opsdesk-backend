package com.opsdesk.common.idempotency;

/**
 * 幂等保护包裹的业务回调。
 *
 * <p>用于让切面把原始 Controller 方法交给 Redis 幂等服务执行，允许透传业务异常和运行时异常。</p>
 */
@FunctionalInterface
public interface IdempotencyCallback {

    Object call() throws Throwable;
}
