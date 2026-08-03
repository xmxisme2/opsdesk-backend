package com.opsdesk.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 用户发起知识库单轮问答的请求参数。 */
public record KnowledgeChatRequest(@NotBlank @Size(max = 2000) String question,
                                   @NotBlank @Size(max = 64) String clientRequestId) { }
