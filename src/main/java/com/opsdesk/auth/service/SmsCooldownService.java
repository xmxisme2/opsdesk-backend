package com.opsdesk.auth.service;

/** 短信发送冷却服务：用 Redis 在手机号与业务场景维度阻止重复发送。 */
public interface SmsCooldownService {

    /** 获取冷却锁，冷却中时抛出含剩余秒数的限流异常。 */
    int acquire(String phone, String scene, int cooldownSeconds);

    /** 外部供应商发送失败时释放冷却锁，允许用户修复配置后重试。 */
    void release(String phone, String scene);
}
