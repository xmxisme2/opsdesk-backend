package com.opsdesk.ai.vo;

import java.time.LocalDateTime;
import java.util.List;

/** AI 会话消息及其引用、反馈。 */
public record AiMessageVO(String id, String role, String content, String status, boolean insufficientEvidence,
                          String feedback, LocalDateTime createTime,
                          List<KnowledgeChatResponseVO.ReferenceVO> references) { }
