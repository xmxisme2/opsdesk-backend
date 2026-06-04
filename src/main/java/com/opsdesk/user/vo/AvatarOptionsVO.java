package com.opsdesk.user.vo;

import java.util.List;

/**
 * 默认头像选项集合。
 *
 * <p>gender 为空表示返回全部性别的头像选项。</p>
 */
public record AvatarOptionsVO(
        String gender,
        List<AvatarOptionVO> options
) {
}
