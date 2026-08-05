package com.opsdesk.ai.vo;

import java.time.LocalDateTime;

/** AI 会话历史列表项。 */
public record AiConversationVO(String id, String title, String status, int messageCount,
                               LocalDateTime lastMessageTime, LocalDateTime createTime) { }
