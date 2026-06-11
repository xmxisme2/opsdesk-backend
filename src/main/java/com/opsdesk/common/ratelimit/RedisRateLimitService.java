package com.opsdesk.common.ratelimit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Redis 固定窗口限流服务。
 *
 * <p>使用 Redis INCR + EXPIRE 实现轻量限流；Redis 异常时降级放行并记录警告，避免风控组件影响核心业务可用性。</p>
 */
@Service
public class RedisRateLimitService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedisRateLimitService.class);

    /** 默认重试等待秒数：Redis TTL 不可用时按当前窗口长度提示前端重试时间。 */
    private static final long DEFAULT_RETRY_AFTER_SECONDS = 1L;

    private final StringRedisTemplate stringRedisTemplate;

    public RedisRateLimitService(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public void check(String key, int limit, long windowSeconds, String message) {
        try {
            Long count = stringRedisTemplate.opsForValue().increment(key);
            if (count == null) {
                return;
            }
            if (count == 1L) {
                stringRedisTemplate.expire(key, Duration.ofSeconds(windowSeconds));
            }
            if (count > limit) {
                throw new RateLimitExceededException(message, resolveRetryAfterSeconds(key, windowSeconds));
            }
        } catch (RateLimitExceededException exception) {
            throw exception;
        } catch (DataAccessException exception) {
            LOGGER.warn("Redis 限流检查失败，已降级放行，key={}", key, exception);
        }
    }

    private long resolveRetryAfterSeconds(String key, long windowSeconds) {
        Long ttl = stringRedisTemplate.getExpire(key, TimeUnit.SECONDS);
        if (ttl == null || ttl < 0) {
            return Math.max(windowSeconds, DEFAULT_RETRY_AFTER_SECONDS);
        }
        return Math.max(ttl, DEFAULT_RETRY_AFTER_SECONDS);
    }
}
