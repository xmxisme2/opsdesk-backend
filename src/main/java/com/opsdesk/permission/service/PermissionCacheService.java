package com.opsdesk.permission.service;

import java.util.Collection;

/**
 * 权限缓存服务。
 *
 * <p>集中清理用户权限和资料缓存，角色权限或用户角色变化后必须调用，避免前端菜单和后端权限上下文使用旧数据。</p>
 */
public interface PermissionCacheService {

    void evictUserPermission(Long userId);

    void evictUserPermissions(Collection<Long> userIds);
}
