package com.opsdesk.ai.vo;

import java.time.LocalDateTime;

/** 独立 AI 服务返回的非敏感运行配置。 */
public record AiRuntimeConfigVO(
        boolean enabled,
        boolean ragEnabled,
        boolean effectiveEnabled,
        boolean effectiveRagEnabled,
        boolean environmentEnabled,
        boolean environmentRagEnabled,
        LocalDateTime updateTime
) {
}
