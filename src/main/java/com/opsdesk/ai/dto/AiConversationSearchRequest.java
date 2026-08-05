package com.opsdesk.ai.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/** 当前用户查询 AI 会话历史的分页参数。 */
public record AiConversationSearchRequest(@Min(1) Integer page, @Min(1) @Max(100) Integer size,
                                          @Size(max = 200) String keyword, Boolean archived) { }
