package com.opsdesk.ai.vo;

/** AI 质量看板顶部指标。 */
public record AiQualitySummaryVO(
        long totalCalls,
        long successCalls,
        long failedCalls,
        long insufficientCalls,
        double successRate,
        double refusalRate,
        long feedbackCount,
        long upFeedbackCount,
        long downFeedbackCount,
        double positiveRate,
        double averageDurationMs,
        long p95DurationMs,
        double averageReferenceCount
) {
}
