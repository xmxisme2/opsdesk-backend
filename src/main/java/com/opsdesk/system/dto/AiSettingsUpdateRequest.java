package com.opsdesk.system.dto;

import jakarta.validation.constraints.NotNull;

/** 管理员更新 AI 开关请求，仅允许调整总开关和知识 RAG 开关。 */
public record AiSettingsUpdateRequest(
        @NotNull(message = "AI 总开关不能为空") Boolean enabled,
        @NotNull(message = "RAG 开关不能为空") Boolean ragEnabled
) {
}
