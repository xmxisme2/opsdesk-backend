package com.opsdesk.common.ratelimit;

/**
 * 接口限流默认规则。
 *
 * <p>集中保存首批接口限流阈值，Controller 注解只引用这些具名常量，避免散落无法解释来源的数字。</p>
 */
public final class RateLimitDefaults {

    /** 一分钟窗口秒数：用于验证码、登录、列表查询和动作接口的短周期限流，不允许外部传入。 */
    public static final int ONE_MINUTE_SECONDS = 60;

    /** 一天窗口秒数：用于短信验证码每日上限，不允许外部传入。 */
    public static final int ONE_DAY_SECONDS = 86_400;

    /** 验证码接口每分钟上限：同 IP 每分钟最多 10 次，防止验证码被刷。 */
    public static final int CAPTCHA_LIMIT_PER_MINUTE = 10;

    /** 登录接口每分钟上限：同 IP + 手机号每分钟最多 5 次，降低暴力破解风险。 */
    public static final int LOGIN_LIMIT_PER_MINUTE = 5;

    /** 短信验证码分钟上限：同 IP + 手机号每分钟最多 1 次，后续短信能力开启后生效。 */
    public static final int SMS_LIMIT_PER_MINUTE = 1;

    /** 短信验证码日上限：同 IP + 手机号每天最多 10 次，后续短信能力开启后生效。 */
    public static final int SMS_LIMIT_PER_DAY = 10;

    /** 列表查询每分钟上限：同登录用户每分钟最多 60 次，覆盖筛选、分页和刷新场景。 */
    public static final int SEARCH_LIMIT_PER_MINUTE = 60;

    /** 关键动作每分钟上限：同登录用户每分钟最多 10 次，覆盖新增、编辑、删除、状态切换和重置密码。 */
    public static final int ACTION_LIMIT_PER_MINUTE = 10;

    private RateLimitDefaults() {
    }
}
