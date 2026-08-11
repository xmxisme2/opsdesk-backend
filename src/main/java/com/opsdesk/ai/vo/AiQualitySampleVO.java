package com.opsdesk.ai.vo;

import java.time.LocalDateTime;

/** 管理员可见的 AI 低质量样本，内容已由独立服务脱敏后持久化。 */
public record AiQualitySampleVO(
        String callId,
        String conversationId,
        String messageId,
        String operatorId,
        String question,
        String answer,
        String result,
        long durationMs,
        int referenceCount,
        String rating,
        String reasonCode,
        String issueReason,
        LocalDateTime createTime
) {
}
