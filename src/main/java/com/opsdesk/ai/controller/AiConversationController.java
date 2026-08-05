package com.opsdesk.ai.controller;

import com.opsdesk.ai.dto.AiConversationSearchRequest;
import com.opsdesk.ai.dto.AiFeedbackRequest;
import com.opsdesk.ai.service.AiKnowledgeChatService;
import com.opsdesk.ai.vo.AiConversationActionVO;
import com.opsdesk.ai.vo.AiConversationDetailVO;
import com.opsdesk.ai.vo.AiConversationVO;
import com.opsdesk.common.response.ApiResponse;
import com.opsdesk.common.response.PageResult;
import com.opsdesk.common.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 当前登录用户访问自己的 AI 会话历史和回答反馈。 */
@RestController
@RequestMapping("/api/ai")
public class AiConversationController {
    private final AiKnowledgeChatService service;
    public AiConversationController(AiKnowledgeChatService service) { this.service = service; }

    @PostMapping("/conversations/search")
    public ApiResponse<PageResult<AiConversationVO>> search(@Valid @RequestBody AiConversationSearchRequest request,
                                                             @AuthenticationPrincipal CurrentUser currentUser) {
        return ApiResponse.success(service.searchConversations(request, currentUser));
    }
    @PostMapping("/conversations/{id}/detail")
    public ApiResponse<AiConversationDetailVO> detail(@PathVariable String id,
                                                       @AuthenticationPrincipal CurrentUser currentUser) {
        return ApiResponse.success(service.conversationDetail(id, currentUser));
    }
    @PostMapping("/conversations/{id}/archive")
    public ApiResponse<AiConversationActionVO> archive(@PathVariable String id,
                                                        @AuthenticationPrincipal CurrentUser currentUser) {
        return ApiResponse.success(service.archiveConversation(id, currentUser));
    }
    @PostMapping("/conversations/{id}/delete")
    public ApiResponse<AiConversationActionVO> delete(@PathVariable String id,
                                                       @AuthenticationPrincipal CurrentUser currentUser) {
        return ApiResponse.success(service.deleteConversation(id, currentUser));
    }
    @PostMapping("/messages/{id}/feedback")
    public ApiResponse<Void> feedback(@PathVariable String id, @Valid @RequestBody AiFeedbackRequest request,
                                      @AuthenticationPrincipal CurrentUser currentUser) {
        service.feedback(id, request, currentUser);
        return ApiResponse.success(null);
    }
}
