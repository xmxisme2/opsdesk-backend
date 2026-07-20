package com.opsdesk.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** 通知模板更新请求，模板变量由服务层按通知类型执行白名单校验。 */
public record NotificationTemplateUpdateRequest(
        @NotBlank @Size(max = 255) String titleTemplate,
        @NotBlank @Size(max = 1000) String contentTemplate,
        @NotNull Boolean enabled
) {}
