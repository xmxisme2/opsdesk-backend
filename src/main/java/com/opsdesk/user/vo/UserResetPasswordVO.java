package com.opsdesk.user.vo;

/**
 * 管理员重置密码返回对象。
 *
 * <p>临时密码仅返回一次，前端需要立即展示给管理员或提示复制后关闭弹窗。</p>
 */
public record UserResetPasswordVO(String temporaryPassword) {
}
