package com.opsdesk.auth.service.impl;

import com.aliyun.dypnsapi20170525.Client;
import com.aliyun.dypnsapi20170525.models.SendSmsVerifyCodeRequest;
import com.aliyun.dypnsapi20170525.models.SendSmsVerifyCodeResponse;
import com.aliyun.teaopenapi.models.Config;
import com.opsdesk.auth.service.SmsVerificationService;
import com.opsdesk.common.exception.BusinessException;
import com.opsdesk.common.exception.ErrorCode;
import com.opsdesk.config.SmsProperties;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 阿里云 Dypnsapi 短信验证码实现。
 *
 * <p>验证码由阿里云动态生成，后续必须调用 CheckSmsVerifyCode 校验，禁止返回或写入验证码明文。</p>
 */
@Service
public class AliyunSmsVerificationService implements SmsVerificationService {

    private final SmsProperties smsProperties;

    public AliyunSmsVerificationService(SmsProperties smsProperties) {
        this.smsProperties = smsProperties;
    }

    @Override
    public void send(String phone, String scene) {
        if (!smsProperties.isEnabled()) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT, "短信验证码能力未启用");
        }
        SmsProperties.AliyunDypnsapiProperties config = smsProperties.getAliyunDypnsapi();
        validateConfig(config);
        try {
            Client client = new Client(new Config()
                    .setAccessKeyId(config.getAccessKeyId())
                    .setAccessKeySecret(config.getAccessKeySecret())
                    .setRegionId(config.getRegionId())
                    .setEndpoint(config.getEndpoint()));
            SendSmsVerifyCodeRequest request = new SendSmsVerifyCodeRequest()
                    .setPhoneNumber(phone)
                    .setSignName(config.getSignName())
                    .setTemplateCode(config.getTemplateCode())
                    .setTemplateParam(config.getTemplateParam())
                    .setCountryCode(config.getCountryCode())
                    .setOutId("opsdesk-" + scene)
                    .setCodeLength(config.getCodeLength().longValue())
                    .setCodeType(config.getCodeType().longValue())
                    .setValidTime(config.getValidTime().longValue())
                    .setInterval(config.getInterval().longValue())
                    .setDuplicatePolicy(config.getDuplicatePolicy().longValue())
                    .setAutoRetry(config.getAutoRetry().longValue())
                    .setReturnVerifyCode(config.isReturnVerifyCode());
            SendSmsVerifyCodeResponse response = client.sendSmsVerifyCode(request);
            if (response.getBody() == null || !Boolean.TRUE.equals(response.getBody().getSuccess())) {
                String message = response.getBody() == null ? "阿里云短信服务无响应" : response.getBody().getMessage();
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "短信发送失败：" + message);
            }
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "短信服务调用失败");
        }
    }

    /** 外部配置缺失时在调用前失败，避免 SDK 抛出包含敏感上下文的异常。 */
    private void validateConfig(SmsProperties.AliyunDypnsapiProperties config) {
        if (!StringUtils.hasText(config.getAccessKeyId()) || !StringUtils.hasText(config.getAccessKeySecret())
                || !StringUtils.hasText(config.getRegionId()) || !StringUtils.hasText(config.getEndpoint())
                || !StringUtils.hasText(config.getSignName()) || !StringUtils.hasText(config.getTemplateCode())
                || !StringUtils.hasText(config.getTemplateParam())) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "阿里云短信配置不完整");
        }
    }
}
