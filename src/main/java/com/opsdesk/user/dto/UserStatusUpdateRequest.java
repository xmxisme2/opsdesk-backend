package com.opsdesk.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * 后台启停用户请求。
 *
 * <p>禁用或锁定用户默认踢出全部登录会话，原因字段仅用于审计日志描述。</p>
 */
@Getter
@Setter
public class UserStatusUpdateRequest {

    @NotBlank(message = "用户状态不能为空")
    private String status;

    @Size(max = 255, message = "状态调整原因不能超过255个字符")
    private String reason;

    private Boolean kickoutSessions = true;
}
