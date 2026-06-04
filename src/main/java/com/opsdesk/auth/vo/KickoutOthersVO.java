package com.opsdesk.auth.vo;

/**
 * 踢出其他会话返回对象。
 *
 * <p>kickedCount 表示本次被失效的 refresh token 会话数量。</p>
 */
public record KickoutOthersVO(int kickedCount) {
}
