package com.opsdesk.ai.dto;

import jakarta.validation.constraints.NotBlank;

/** 索引对账任务请求。 */
public record IndexReconcileRequest(@NotBlank String clientRequestId) {
}
