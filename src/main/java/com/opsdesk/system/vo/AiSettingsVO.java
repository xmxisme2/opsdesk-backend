package com.opsdesk.system.vo;

import java.time.LocalDateTime;

/** AI 公开运行设置；包含期望状态、最终状态和环境放行状态，不包含任何密钥。 */
public record AiSettingsVO(boolean enabled, String provider, String model,
                           boolean ragEnabled, boolean effectiveEnabled, boolean effectiveRagEnabled,
                           boolean environmentEnabled, boolean environmentRagEnabled,
                           String disclaimer, LocalDateTime updateTime) {
}
