package com.opsdesk.common.ratelimit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Redis 限流服务测试。
 *
 * <p>覆盖固定窗口计数、首次过期时间设置和超限重试时间返回，避免限流规则在高频接口上失效。</p>
 */
@ExtendWith(MockitoExtension.class)
class RedisRateLimitServiceTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private RedisRateLimitService rateLimitService;

    @BeforeEach
    void setUp() {
        rateLimitService = new RedisRateLimitService(stringRedisTemplate);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void checkShouldSetExpireWhenFirstHit() {
        when(valueOperations.increment("rate_limit:test")).thenReturn(1L);

        rateLimitService.check("rate_limit:test", 5, 60, "操作过于频繁，请稍后再试");

        verify(stringRedisTemplate).expire("rate_limit:test", Duration.ofSeconds(60));
    }

    @Test
    void checkShouldRejectWhenCountExceedsLimit() {
        when(valueOperations.increment("rate_limit:test")).thenReturn(6L);
        when(stringRedisTemplate.getExpire("rate_limit:test", TimeUnit.SECONDS)).thenReturn(25L);

        assertThatThrownBy(() -> rateLimitService.check("rate_limit:test", 5, 60, "操作过于频繁，请稍后再试"))
                .isInstanceOf(RateLimitExceededException.class)
                .extracting("retryAfterSeconds")
                .isEqualTo(25L);
    }
}
