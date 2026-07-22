package com.opsdesk.auth.service.impl;

import com.opsdesk.auth.service.LoginFailureLockService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Redis 登录失败风控实现。
 *
 * <p>同一手机号连续 5 次密码失败会由上层锁定账号；同一 IP 连续 5 次失败则在 Redis
 * 中临时锁定 15 分钟。Redis 不可用时降级放行，避免影响登录可用性。</p>
 */
@Service
public class RedisLoginFailureLockService implements LoginFailureLockService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedisLoginFailureLockService.class);
    /** 账号失败计数 Key 前缀：仅服务端拼接，外部不可传入。 */
    private static final String ACCOUNT_FAILURE_KEY_PREFIX = "auth:login:failure:account:";
    /** IP 失败计数 Key 前缀：仅服务端拼接，外部不可传入。 */
    private static final String IP_FAILURE_KEY_PREFIX = "auth:login:failure:ip:";
    /** IP 临时锁定 Key 前缀：达到阈值后写入，外部不可传入。 */
    private static final String IP_LOCK_KEY_PREFIX = "auth:login:lock:ip:";
    /** 连续失败阈值：产品约定为 5 次，不允许接口侧覆盖。 */
    private static final long FAILURE_THRESHOLD = 5L;
    /** 连续失败统计窗口及 IP 临时锁定时长：15 分钟。 */
    private static final Duration FAILURE_WINDOW = Duration.ofMinutes(15);

    private final StringRedisTemplate stringRedisTemplate;

    public RedisLoginFailureLockService(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean isIpLocked(String requestIp) {
        try {
            return Boolean.TRUE.equals(stringRedisTemplate.hasKey(ipLockKey(requestIp)));
        } catch (DataAccessException exception) {
            LOGGER.warn("登录 IP 锁定检查失败，已降级放行，ip={}", requestIp, exception);
            return false;
        }
    }

    @Override
    public boolean recordPasswordFailure(String phone, String requestIp) {
        try {
            Long accountFailures = incrementWithExpiry(accountFailureKey(phone));
            Long ipFailures = incrementWithExpiry(ipFailureKey(requestIp));
            if (ipFailures != null && ipFailures >= FAILURE_THRESHOLD) {
                stringRedisTemplate.opsForValue().set(ipLockKey(requestIp), "1", FAILURE_WINDOW);
            }
            return accountFailures != null && accountFailures >= FAILURE_THRESHOLD;
        } catch (DataAccessException exception) {
            LOGGER.warn("登录失败计数写入失败，已降级跳过锁定，phone={}, ip={}", phone, requestIp, exception);
            return false;
        }
    }

    @Override
    public void clearFailures(String phone, String requestIp) {
        try {
            stringRedisTemplate.delete(accountFailureKey(phone));
            stringRedisTemplate.delete(ipFailureKey(requestIp));
            stringRedisTemplate.delete(ipLockKey(requestIp));
        } catch (DataAccessException exception) {
            LOGGER.warn("登录成功后的失败计数清理失败，phone={}, ip={}", phone, requestIp, exception);
        }
    }

    @Override
    public void clearAccountFailures(String phone) {
        try {
            stringRedisTemplate.delete(accountFailureKey(phone));
        } catch (DataAccessException exception) {
            LOGGER.warn("管理员解锁后的账号失败计数清理失败，phone={}", phone, exception);
        }
    }

    private Long incrementWithExpiry(String key) {
        Long count = stringRedisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            stringRedisTemplate.expire(key, FAILURE_WINDOW);
        }
        return count;
    }

    private String accountFailureKey(String phone) {
        return ACCOUNT_FAILURE_KEY_PREFIX + phone;
    }

    private String ipFailureKey(String requestIp) {
        return IP_FAILURE_KEY_PREFIX + normalizeIp(requestIp);
    }

    private String ipLockKey(String requestIp) {
        return IP_LOCK_KEY_PREFIX + normalizeIp(requestIp);
    }

    private String normalizeIp(String requestIp) {
        return requestIp == null || requestIp.isBlank() ? "unknown" : requestIp.trim();
    }
}
