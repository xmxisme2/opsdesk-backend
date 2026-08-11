package com.opsdesk.system.vo;

/** AI 公开运行设置；只包含开关、供应商、模型和免责声明，不包含任何密钥。 */
public record AiSettingsVO(boolean enabled, String provider, String model,
                           boolean ragEnabled, String disclaimer) {
}
