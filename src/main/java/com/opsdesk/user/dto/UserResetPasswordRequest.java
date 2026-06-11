package com.opsdesk.user.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * 管理员重置密码请求。
 *
 * <p>newPassword 可选；未传时服务层生成一次性临时密码并只在接口响应中返回。</p>
 */
@Getter
@Setter
public class UserResetPasswordRequest {

    @Size(min = 8, max = 64, message = "新密码长度必须在 8 到 64 位之间")
    private String newPassword;
}
