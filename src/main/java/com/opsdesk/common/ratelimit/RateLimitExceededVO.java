package com.opsdesk.common.ratelimit;

/**
 * 限流命中返回对象。
 *
 * <p>retryAfterSeconds 告诉前端建议等待多久后重试，便于页面展示倒计时或提示。</p>
 */
public record RateLimitExceededVO(long retryAfterSeconds) {
}
