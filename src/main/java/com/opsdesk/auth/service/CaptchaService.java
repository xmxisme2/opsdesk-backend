package com.opsdesk.auth.service;

import com.opsdesk.auth.dto.CaptchaRequest;
import com.opsdesk.auth.vo.CaptchaVO;

/**
 * 图形验证码服务。
 *
 * <p>首版验证码用于登录保护，验证码答案保存在 Redis 并按一次性口径校验。</p>
 */
public interface CaptchaService {

    CaptchaVO createCaptcha(CaptchaRequest request);

    void validate(String captchaId, String captchaCode);
}
