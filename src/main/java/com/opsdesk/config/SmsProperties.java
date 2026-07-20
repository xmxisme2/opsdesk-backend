package com.opsdesk.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 短信验证码配置。
 *
 * <p>密钥仅允许由本地配置或环境变量提供，禁止写入业务代码和日志。</p>
 */
@Component
@ConfigurationProperties(prefix = "opsdesk.sms")
public class SmsProperties {

    /** 短信发送总开关：未完成联调或故障时可快速关闭外部调用。 */
    private boolean enabled;

    /** 阿里云号码认证服务配置：仅用于验证码短信，不复用普通营销短信通道。 */
    private AliyunDypnsapiProperties aliyunDypnsapi = new AliyunDypnsapiProperties();

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public AliyunDypnsapiProperties getAliyunDypnsapi() { return aliyunDypnsapi; }
    public void setAliyunDypnsapi(AliyunDypnsapiProperties aliyunDypnsapi) { this.aliyunDypnsapi = aliyunDypnsapi; }

    /** 阿里云 Dypnsapi 的认证、模板和验证码策略配置。 */
    public static class AliyunDypnsapiProperties {
        private String regionId;
        private String endpoint;
        private String accessKeyId;
        private String accessKeySecret;
        private String signName;
        private String templateCode;
        private String templateParam;
        private String countryCode;
        private String testPhoneNumber;
        private Integer codeLength;
        private Integer codeType;
        private Integer validTime;
        private Integer interval;
        private Integer duplicatePolicy;
        private Integer autoRetry;
        private boolean returnVerifyCode;

        public String getRegionId() { return regionId; }
        public void setRegionId(String regionId) { this.regionId = regionId; }
        public String getEndpoint() { return endpoint; }
        public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
        public String getAccessKeyId() { return accessKeyId; }
        public void setAccessKeyId(String accessKeyId) { this.accessKeyId = accessKeyId; }
        public String getAccessKeySecret() { return accessKeySecret; }
        public void setAccessKeySecret(String accessKeySecret) { this.accessKeySecret = accessKeySecret; }
        public String getSignName() { return signName; }
        public void setSignName(String signName) { this.signName = signName; }
        public String getTemplateCode() { return templateCode; }
        public void setTemplateCode(String templateCode) { this.templateCode = templateCode; }
        public String getTemplateParam() { return templateParam; }
        public void setTemplateParam(String templateParam) { this.templateParam = templateParam; }
        public String getCountryCode() { return countryCode; }
        public void setCountryCode(String countryCode) { this.countryCode = countryCode; }
        public String getTestPhoneNumber() { return testPhoneNumber; }
        public void setTestPhoneNumber(String testPhoneNumber) { this.testPhoneNumber = testPhoneNumber; }
        public Integer getCodeLength() { return codeLength; }
        public void setCodeLength(Integer codeLength) { this.codeLength = codeLength; }
        public Integer getCodeType() { return codeType; }
        public void setCodeType(Integer codeType) { this.codeType = codeType; }
        public Integer getValidTime() { return validTime; }
        public void setValidTime(Integer validTime) { this.validTime = validTime; }
        public Integer getInterval() { return interval; }
        public void setInterval(Integer interval) { this.interval = interval; }
        public Integer getDuplicatePolicy() { return duplicatePolicy; }
        public void setDuplicatePolicy(Integer duplicatePolicy) { this.duplicatePolicy = duplicatePolicy; }
        public Integer getAutoRetry() { return autoRetry; }
        public void setAutoRetry(Integer autoRetry) { this.autoRetry = autoRetry; }
        public boolean isReturnVerifyCode() { return returnVerifyCode; }
        public void setReturnVerifyCode(boolean returnVerifyCode) { this.returnVerifyCode = returnVerifyCode; }
    }
}
