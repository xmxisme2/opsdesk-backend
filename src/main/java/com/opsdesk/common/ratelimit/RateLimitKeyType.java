package com.opsdesk.common.ratelimit;

/**
 * 限流身份维度。
 *
 * <p>用于声明接口按什么主体计数，公开接口通常按 IP 或 IP + 手机号，登录接口优先按用户 ID。</p>
 */
public enum RateLimitKeyType {

    /** 按客户端 IP 限流：适合验证码等未登录且无手机号参数的公开接口。 */
    IP,

    /** 按当前登录用户 ID 限流：适合已登录业务接口，不允许外部直接传入用户 ID。 */
    USER,

    /** 按客户端 IP + 手机号限流：适合登录、注册和短信验证码等公开接口，手机号从请求 DTO 中读取。 */
    IP_AND_PHONE,

    /** 按手机号限流：适合只关心手机号频次的公开接口，手机号从请求 DTO 中读取。 */
    PHONE,

    /** 自动限流维度：有登录用户时按用户 ID，否则按客户端 IP。 */
    AUTO
}
