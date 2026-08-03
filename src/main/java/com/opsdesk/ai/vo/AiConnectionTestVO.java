package com.opsdesk.ai.vo;

/** AI 模型或连接测试摘要，不包含任何敏感配置。 */
public record AiConnectionTestVO(
        String target,
        boolean configured,
        boolean success,
        String provider,
        String model,
        Integer dimensions,
        long latencyMs,
        String message
) {
}
