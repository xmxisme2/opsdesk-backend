package com.opsdesk.common.idempotency;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opsdesk.common.exception.BusinessException;
import com.opsdesk.common.exception.ErrorCode;
import com.opsdesk.common.response.ApiResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.UUID;

/**
 * Redis 幂等保护服务。
 *
 * <p>通过短期执行锁阻止并发重复写入，通过成功响应缓存复用首次结果，适用于创建、删除、重置密码和状态流转等关键动作。</p>
 */
@Service
public class RedisIdempotencyService {

    /** 幂等成功结果 Key 前缀：缓存首次成功响应，重复请求直接复用。 */
    private static final String RESULT_KEY_PREFIX = "idempotency:result:";

    /** 幂等执行锁 Key 前缀：首个请求执行期间阻止同 Key 请求重复进入业务方法。 */
    private static final String LOCK_KEY_PREFIX = "idempotency:lock:";

    /** 执行锁占位值：仅用于标记请求进行中，不允许外部传入。 */
    private static final String LOCK_VALUE_PREFIX = "RUNNING:";

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public RedisIdempotencyService(StringRedisTemplate stringRedisTemplate, ObjectMapper objectMapper) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
    }

    public Object execute(String key,
                          long resultTtlSeconds,
                          long lockTtlSeconds,
                          IdempotencyCallback callback) throws Throwable {
        String resultKey = RESULT_KEY_PREFIX + key;
        String lockKey = LOCK_KEY_PREFIX + key;
        String cachedResult = stringRedisTemplate.opsForValue().get(resultKey);
        if (StringUtils.hasText(cachedResult)) {
            return toCachedApiResponse(cachedResult);
        }

        Boolean locked = stringRedisTemplate.opsForValue().setIfAbsent(
                lockKey,
                LOCK_VALUE_PREFIX + UUID.randomUUID(),
                Duration.ofSeconds(lockTtlSeconds)
        );
        if (!Boolean.TRUE.equals(locked)) {
            String latestResult = stringRedisTemplate.opsForValue().get(resultKey);
            if (StringUtils.hasText(latestResult)) {
                return toCachedApiResponse(latestResult);
            }
            throw new BusinessException(ErrorCode.STATE_CONFLICT, "重复请求正在处理中，请稍后重试");
        }

        try {
            Object result = callback.call();
            cacheSuccessfulApiResponse(resultKey, result, resultTtlSeconds);
            return result;
        } finally {
            stringRedisTemplate.delete(lockKey);
        }
    }

    private void cacheSuccessfulApiResponse(String resultKey, Object result, long resultTtlSeconds) {
        if (!(result instanceof ApiResponse<?> response) || response.getCode() != 200) {
            return;
        }
        try {
            String json = objectMapper.writeValueAsString(response);
            stringRedisTemplate.opsForValue().set(resultKey, json, Duration.ofSeconds(resultTtlSeconds));
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "幂等结果缓存失败");
        }
    }

    private ApiResponse<?> toCachedApiResponse(String cachedJson) {
        try {
            JsonNode root = objectMapper.readTree(cachedJson);
            JsonNode data = root.get("data");
            if (data == null || data.isNull()) {
                return ApiResponse.success();
            }
            return ApiResponse.success(data);
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "幂等结果读取失败");
        }
    }
}
