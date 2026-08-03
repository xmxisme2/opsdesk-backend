package com.opsdesk.ai.internal.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;

/** AI 全量重建按游标读取已发布文章的内部请求。 */
public record PublishedSnapshotPageRequest(String afterId, @Positive @Max(200) Integer limit) {
}
