package com.opsdesk.user.vo;

/**
 * 用户角色简要返回对象。
 *
 * <p>登录和当前用户接口只需要角色 ID、编码和名称，完整角色管理后续由角色模块补充。</p>
 */
public record RoleBriefVO(
        String id,
        String code,
        String name
) {
}
