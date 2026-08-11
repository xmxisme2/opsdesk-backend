package com.opsdesk.ai.dto;

import java.time.LocalDate;

/** AI 质量统计时间范围，独立服务默认最近 30 天并限制最大 90 天。 */
public record AiQualityRangeRequest(LocalDate dateFrom, LocalDate dateTo) {
}
