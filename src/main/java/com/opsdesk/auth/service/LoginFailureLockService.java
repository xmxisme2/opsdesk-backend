package com.opsdesk.auth.service;

/**
 * 登录失败风控服务。
 *
 * <p>仅保存短期失败计数和 IP 临时锁定状态；账号的最终锁定状态由认证服务写入
 * {@code sys_user.status}，并由管理员通过用户管理页面解除。</p>
 */
public interface LoginFailureLockService {

    /** 判断来源 IP 是否处于临时锁定期，命中后不再继续校验密码或验证码。 */
    boolean isIpLocked(String requestIp);

    /** 记录一次密码不匹配，返回该手机号是否刚达到账号锁定阈值。 */
    boolean recordPasswordFailure(String phone, String requestIp);

    /** 登录成功后清理手机号和当前来源 IP 的连续失败记录。 */
    void clearFailures(String phone, String requestIp);

    /** 管理员解除账号锁定时清理该手机号的失败记录。 */
    void clearAccountFailures(String phone);
}
