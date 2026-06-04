package com.opsdesk.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 刷新令牌请求。
 *
 * <p>access token 过期后，前端使用 refresh token 换取新的登录令牌。</p>
 */
public class RefreshTokenRequest {

    @NotBlank(message = "刷新令牌不能为空")
    private String refreshToken;

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}
