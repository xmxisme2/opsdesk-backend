package com.opsdesk.ai.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/** AI 低质量样本分页筛选参数。 */
public record AiQualitySampleSearchRequest(
        @Min(1) Integer page,
        @Min(1) @Max(100) Integer size,
        LocalDate dateFrom,
        LocalDate dateTo,
        @Size(max = 32) String result,
        @Size(max = 64) String reasonCode,
        @Size(max = 200) String keyword
) {
}
