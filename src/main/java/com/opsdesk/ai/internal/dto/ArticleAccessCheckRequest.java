package com.opsdesk.ai.internal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/** AI 引用返回前批量确认文章访问权限的内部请求。 */
public record ArticleAccessCheckRequest(
        @NotBlank String userId,
        @NotEmpty List<String> articleIds,
        String requiredStatus
) {
}
