package com.opsdesk.auth.model;

/**
 * JWT 载荷中的业务声明。
 *
 * <p>只保留认证必要字段，角色和权限每次从数据库或缓存加载，避免 token 中权限过期。</p>
 */
public record TokenClaims(
        String tokenId,
        Long userId,
        String tokenType,
        long issuedAt,
        long expiresAt,
        boolean rememberMe
) {
}
