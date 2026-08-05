package com.opsdesk.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 用户发起知识库问答的请求参数，可传当前用户自己的会话 ID 继续追问。 */
public record KnowledgeChatRequest(@NotBlank @Size(max = 2000) String question,
                                   String conversationId,
                                   @NotBlank @Size(max = 64) String clientRequestId) { }
