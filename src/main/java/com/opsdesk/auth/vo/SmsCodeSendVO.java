package com.opsdesk.auth.vo;

/**
 * 短信验证码预留入口返回对象。
 *
 * <p>首版短信能力关闭，前端可根据 enabled=false 隐藏或禁用短信入口。</p>
 */
public record SmsCodeSendVO(
        boolean enabled,
        String message
) {
}
