package com.opsdesk.ai.vo;

import java.util.List;

/** AI 会话详情。 */
public record AiConversationDetailVO(AiConversationVO conversation, List<AiMessageVO> messages) { }
