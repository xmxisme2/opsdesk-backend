package com.opsdesk.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 踢出其他登录会话请求。
 *
 * <p>当前 refresh token 用于识别需要保留的会话，其余会话统一失效。</p>
 */
public class KickoutOthersRequest {

    @NotBlank(message = "当前刷新令牌不能为空")
    private String currentRefreshToken;

    public String getCurrentRefreshToken() {
        return currentRefreshToken;
    }

    public void setCurrentRefreshToken(String currentRefreshToken) {
        this.currentRefreshToken = currentRefreshToken;
    }
}
