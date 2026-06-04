package com.opsdesk.auth.vo;

/**
 * 图形验证码返回对象。
 *
 * <p>imageBase64 直接给前端 img 标签展示，expiresIn 表示验证码有效秒数。</p>
 */
public record CaptchaVO(
        String captchaId,
        String imageBase64,
        int expiresIn
) {
}
