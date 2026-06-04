package com.opsdesk.auth.model;

/**
 * 登录令牌签发结果。
 *
 * <p>包含 access token 和 refresh token，以及前端需要展示或存储的过期秒数。</p>
 */
public record TokenPair(
        String accessToken,
        long expiresIn,
        String refreshToken,
        long refreshExpiresIn
) {
}
