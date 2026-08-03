package com.opsdesk.ai.service;

import com.opsdesk.ai.dto.KnowledgeChatRequest;
import com.opsdesk.ai.vo.KnowledgeChatResponseVO;
import com.opsdesk.common.security.CurrentUser;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

/** 主应用向独立 AI 服务代理单轮知识问答的服务。 */
public interface AiKnowledgeChatService {
    KnowledgeChatResponseVO chat(KnowledgeChatRequest request, CurrentUser currentUser);
    StreamingResponseBody stream(KnowledgeChatRequest request, CurrentUser currentUser);
}
