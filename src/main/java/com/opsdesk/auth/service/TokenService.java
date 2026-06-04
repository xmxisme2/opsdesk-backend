package com.opsdesk.auth.service;

import com.opsdesk.auth.model.TokenClaims;
import com.opsdesk.auth.model.TokenPair;

/**
 * 登录令牌服务。
 *
 * <p>负责 JWT 签发、校验、刷新、黑名单和多端会话失效。</p>
 */
public interface TokenService {

    TokenPair issueTokenPair(Long userId, boolean rememberMe);

    TokenClaims parseAccessToken(String token);

    TokenClaims parseRefreshToken(String token);

    TokenPair refresh(String refreshToken);

    void logoutAccessToken(String accessToken);

    int kickoutOtherSessions(Long userId, String currentRefreshToken);

    int invalidateAllRefreshSessions(Long userId);
}
