package com.opsdesk.ai.vo;

import java.time.LocalDate;

/** AI 质量趋势单日数据。 */
public record AiQualityTrendVO(LocalDate date, long totalCalls, long successCalls,
                               long insufficientCalls, long failedCalls, long negativeFeedbackCount) {
}
