package com.opsdesk.user.vo;

import java.util.List;

/**
 * 用户资料返回对象。
 *
 * <p>ID 统一按字符串传输，roles 和 permissions 供前端菜单、按钮和路由守卫使用。</p>
 */
public record UserVO(
        String id,
        String username,
        String nickname,
        String email,
        String phone,
        String gender,
        String avatarCode,
        String avatarUrl,
        String departmentId,
        String departmentName,
        List<RoleBriefVO> roles,
        List<String> permissions,
        String status,
        String createdAt,
        String updatedAt
) {
}
