package com.opsdesk.auth.service;

/** 阿里云短信验证码服务：仅负责供应商调用，不处理登录或注册业务判断。 */
public interface SmsVerificationService {

    /** 向指定手机号发送由阿里云动态生成的验证码。 */
    void send(String phone, String scene);
}
