package com.opsdesk.ai.vo;

import java.time.LocalDateTime;
import java.util.List;

/** 主应用对外返回的持久化 RAG JSON 降级结果。 */
public record KnowledgeChatResponseVO(String answer, String conversationId, String messageId,
                                      boolean insufficientEvidence, List<ReferenceVO> references,
                                      String disclaimer, LocalDateTime generatedAt) {
    /** 已经 AI 服务与主应用双重约束的文章引用。 */
    public record ReferenceVO(String articleId, String title, String heading, String snippet, double score) { }
}
