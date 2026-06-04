package com.opsdesk.auth.vo;

import com.opsdesk.user.vo.UserVO;

/**
 * 登录成功返回对象。
 *
 * <p>同时返回令牌和用户上下文，前端据此恢复菜单、角色和权限。</p>
 */
public record LoginResultVO(
        String accessToken,
        String tokenType,
        long expiresIn,
        String refreshToken,
        long refreshExpiresIn,
        UserVO user
) {
}
