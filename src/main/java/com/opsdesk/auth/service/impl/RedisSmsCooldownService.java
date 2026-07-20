package com.opsdesk.auth.service.impl;

import com.opsdesk.auth.service.SmsCooldownService;
import com.opsdesk.common.ratelimit.RateLimitExceededException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/** Redis 短信冷却实现：不同 IP 访问同一手机号也共用 60 秒冷却窗口。 */
@Service
public class RedisSmsCooldownService implements SmsCooldownService {

    /** 短信冷却 Key 前缀：按业务场景和手机号隔离，不包含验证码明文。 */
    private static final String SMS_COOLDOWN_KEY_PREFIX = "sms:cooldown:";

    private final StringRedisTemplate stringRedisTemplate;

    public RedisSmsCooldownService(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public int acquire(String phone, String scene, int cooldownSeconds) {
        String key = key(phone, scene);
        Boolean acquired = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", Duration.ofSeconds(cooldownSeconds));
        if (Boolean.TRUE.equals(acquired)) {
            return cooldownSeconds;
        }
        Long seconds = stringRedisTemplate.getExpire(key, TimeUnit.SECONDS);
        long retryAfterSeconds = seconds == null || seconds < 1 ? 1 : seconds;
        throw new RateLimitExceededException("验证码已发送，请稍后再试", retryAfterSeconds);
    }

    @Override
    public void release(String phone, String scene) {
        stringRedisTemplate.delete(key(phone, scene));
    }

    private String key(String phone, String scene) {
        return SMS_COOLDOWN_KEY_PREFIX + scene + ':' + phone;
    }
}
