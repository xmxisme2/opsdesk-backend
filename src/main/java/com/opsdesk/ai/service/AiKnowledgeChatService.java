package com.opsdesk.ai.service;

import com.opsdesk.ai.dto.KnowledgeChatRequest;
import com.opsdesk.ai.dto.AiConversationSearchRequest;
import com.opsdesk.ai.dto.AiFeedbackRequest;
import com.opsdesk.ai.vo.AiConversationActionVO;
import com.opsdesk.ai.vo.AiConversationDetailVO;
import com.opsdesk.ai.vo.AiConversationVO;
import com.opsdesk.ai.vo.KnowledgeChatResponseVO;
import com.opsdesk.common.response.PageResult;
import com.opsdesk.common.security.CurrentUser;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

/** 主应用向独立 AI 服务代理知识问答、会话历史和反馈的服务。 */
public interface AiKnowledgeChatService {
    KnowledgeChatResponseVO chat(KnowledgeChatRequest request, CurrentUser currentUser);
    StreamingResponseBody stream(KnowledgeChatRequest request, CurrentUser currentUser);
    PageResult<AiConversationVO> searchConversations(AiConversationSearchRequest request, CurrentUser currentUser);
    AiConversationDetailVO conversationDetail(String id, CurrentUser currentUser);
    AiConversationActionVO archiveConversation(String id, CurrentUser currentUser);
    AiConversationActionVO deleteConversation(String id, CurrentUser currentUser);
    void feedback(String messageId, AiFeedbackRequest request, CurrentUser currentUser);
}
