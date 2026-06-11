package com.opsdesk.common.ratelimit;

/**
 * 接口限流异常。
 *
 * <p>由限流切面抛出，全局异常处理器会转换成业务错误码 429001 并返回重试等待秒数。</p>
 */
public class RateLimitExceededException extends RuntimeException {

    private final long retryAfterSeconds;

    public RateLimitExceededException(String message, long retryAfterSeconds) {
        super(message);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
