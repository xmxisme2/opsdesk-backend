package com.opsdesk.common.idempotency;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opsdesk.common.exception.BusinessException;
import com.opsdesk.common.exception.ErrorCode;
import com.opsdesk.common.response.ApiResponse;
import com.opsdesk.user.vo.UserResetPasswordVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Redis 幂等服务测试。
 *
 * <p>覆盖同一幂等 Key 的结果复用和进行中请求拦截，防止重置密码、删除、状态流转等关键动作被重复执行。</p>
 */
@ExtendWith(MockitoExtension.class)
class RedisIdempotencyServiceTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private RedisIdempotencyService idempotencyService;

    @BeforeEach
    void setUp() {
        idempotencyService = new RedisIdempotencyService(stringRedisTemplate, new ObjectMapper());
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void executeShouldCacheSuccessfulResponseAndReuseItWithoutRunningBusinessAgain() throws Throwable {
        String cachedJson = "{\"code\":200,\"message\":\"success\",\"data\":{\"temporaryPassword\":\"tmp-001\"}}";
        when(valueOperations.get("idempotency:result:test")).thenReturn(null).thenReturn(cachedJson);
        when(valueOperations.setIfAbsent(
                eq("idempotency:lock:test"),
                anyString(),
                eq(Duration.ofSeconds(30))
        )).thenReturn(true);
        AtomicInteger executions = new AtomicInteger();

        Object first = idempotencyService.execute("test", 600, 30, () -> {
            executions.incrementAndGet();
            return ApiResponse.success(new UserResetPasswordVO("tmp-001"));
        });
        Object second = idempotencyService.execute("test", 600, 30, () -> {
            executions.incrementAndGet();
            return ApiResponse.success(new UserResetPasswordVO("tmp-002"));
        });

        assertThat(first).isInstanceOf(ApiResponse.class);
        assertThat(second).isInstanceOf(ApiResponse.class);
        assertThat(executions).hasValue(1);
        ApiResponse<?> cachedResponse = (ApiResponse<?>) second;
        assertThat(cachedResponse.getCode()).isEqualTo(200);
        assertThat(cachedResponse.getData()).isInstanceOf(JsonNode.class);
        assertThat(((JsonNode) cachedResponse.getData()).get("temporaryPassword").asText()).isEqualTo("tmp-001");
        verify(valueOperations).set(
                eq("idempotency:result:test"),
                anyString(),
                eq(Duration.ofSeconds(600))
        );
        verify(stringRedisTemplate).delete("idempotency:lock:test");
    }

    @Test
    void executeShouldRejectConcurrentDuplicateWhenResultIsNotReady() {
        when(valueOperations.get("idempotency:result:test")).thenReturn(null).thenReturn(null);
        when(valueOperations.setIfAbsent(
                eq("idempotency:lock:test"),
                anyString(),
                eq(Duration.ofSeconds(30))
        )).thenReturn(false);

        assertThatThrownBy(() -> idempotencyService.execute(
                "test",
                600,
                30,
                () -> ApiResponse.success(new UserResetPasswordVO("tmp-001"))
        ))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.STATE_CONFLICT);
    }
}
