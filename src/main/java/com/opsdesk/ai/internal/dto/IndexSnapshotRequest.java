package com.opsdesk.ai.internal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** AI 服务读取文章索引快照的内部请求。 */
public record IndexSnapshotRequest(
        @NotNull @Positive Long expectedVersion,
        @NotBlank String eventId
) {
}
