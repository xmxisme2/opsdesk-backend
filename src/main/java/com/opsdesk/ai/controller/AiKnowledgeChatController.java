package com.opsdesk.ai.controller;

import com.opsdesk.ai.dto.KnowledgeChatRequest;
import com.opsdesk.ai.service.AiKnowledgeChatService;
import com.opsdesk.ai.vo.KnowledgeChatResponseVO;
import com.opsdesk.common.idempotency.Idempotent;
import com.opsdesk.common.response.ApiResponse;
import com.opsdesk.common.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import org.springframework.http.MediaType;

/** 已登录用户访问的知识库 RAG JSON 降级接口。 */
@RestController
@RequestMapping("/api/ai/knowledge")
public class AiKnowledgeChatController {
    private final AiKnowledgeChatService service;
    public AiKnowledgeChatController(AiKnowledgeChatService service) { this.service = service; }
    @PostMapping("/chat")
    @Idempotent
    public ApiResponse<KnowledgeChatResponseVO> chat(@Valid @RequestBody KnowledgeChatRequest request,
                                                     @AuthenticationPrincipal CurrentUser currentUser) {
        return ApiResponse.success(service.chat(request, currentUser));
    }

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public StreamingResponseBody stream(@Valid @RequestBody KnowledgeChatRequest request,
                                        @AuthenticationPrincipal CurrentUser currentUser) {
        return service.stream(request, currentUser);
    }
}
