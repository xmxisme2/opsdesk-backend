package com.opsdesk.ai.vo;

import java.time.LocalDate;
import java.util.List;

/** AI 质量看板总览。 */
public record AiQualityOverviewVO(
        LocalDate dateFrom,
        LocalDate dateTo,
        AiQualitySummaryVO summary,
        List<AiQualityTrendVO> trends,
        List<AiQualityDistributionVO> resultDistribution,
        List<AiQualityDistributionVO> feedbackReasons
) {
}
