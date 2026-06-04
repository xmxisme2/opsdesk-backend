package com.opsdesk.auth.service.impl;

import com.opsdesk.common.exception.BusinessException;
import com.opsdesk.common.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 图形验证码服务测试。
 */
@ExtendWith(MockitoExtension.class)
class CaptchaServiceImplTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private CaptchaServiceImpl captchaService;

    @BeforeEach
    void setUp() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        captchaService = new CaptchaServiceImpl(stringRedisTemplate);
    }

    @Test
    void validateShouldDeleteCaptchaWhenCodeMatches() {
        when(valueOperations.get("captcha:test-id")).thenReturn("A7K9");

        captchaService.validate("test-id", "a7k9");

        verify(stringRedisTemplate).delete("captcha:test-id");
    }

    @Test
    void validateShouldRejectExpiredCaptcha() {
        when(valueOperations.get("captcha:expired-id")).thenReturn(null);

        assertThatThrownBy(() -> captchaService.validate("expired-id", "A7K9"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PARAM_ERROR);
    }
}
