package com.opsdesk.system.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** 邮件通知配置更新请求；默认收件邮箱仅在开关打开后参与投递。 */
public record EmailNotificationSettingsUpdateRequest(
        boolean enabled,
        @NotBlank @Email String defaultRecipient
) {}
