package com.opsdesk.user.vo;

/**
 * 默认头像选项。
 *
 * <p>注册和个人资料页按性别展示可选头像，后续可替换为真实静态资源地址。</p>
 */
public record AvatarOptionVO(
        String avatarCode,
        String avatarUrl,
        String label
) {
}
