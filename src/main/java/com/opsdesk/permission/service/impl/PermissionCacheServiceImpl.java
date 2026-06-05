package com.opsdesk.permission.service.impl;

import com.opsdesk.permission.service.PermissionCacheService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 权限缓存服务实现。
 *
 * <p>当前清理 `permission:user:{userId}` 和 `user:profile:{userId}` 两类缓存，后续如果扩展菜单缓存可继续在此集中维护。</p>
 */
@Service
public class PermissionCacheServiceImpl implements PermissionCacheService {

    /** 用户权限缓存前缀：角色权限或用户角色变化后按用户 ID 删除该缓存。 */
    private static final String PERMISSION_CACHE_PREFIX = "permission:user:";

    /** 用户资料缓存前缀：角色变化会影响用户 VO 中的角色和权限摘要，需要同步失效。 */
    private static final String USER_PROFILE_CACHE_PREFIX = "user:profile:";

    private final StringRedisTemplate stringRedisTemplate;

    public PermissionCacheServiceImpl(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public void evictUserPermission(Long userId) {
        if (userId == null) {
            return;
        }
        stringRedisTemplate.delete(List.of(
                PERMISSION_CACHE_PREFIX + userId,
                USER_PROFILE_CACHE_PREFIX + userId
        ));
    }

    @Override
    public void evictUserPermissions(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return;
        }
        List<String> keys = new ArrayList<>();
        userIds.stream()
                .filter(id -> id != null)
                .distinct()
                .forEach(id -> {
                    keys.add(PERMISSION_CACHE_PREFIX + id);
                    keys.add(USER_PROFILE_CACHE_PREFIX + id);
                });
        if (!keys.isEmpty()) {
            stringRedisTemplate.delete(keys);
        }
    }
}
