package com.opsdesk.ai.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.NotBlank;

/** AI 模型或基础连接测试请求。 */
public record AiConnectionTestRequest(
        @NotBlank @Pattern(regexp = "CHAT|EMBEDDING|OPENSEARCH") String target
) {
}
