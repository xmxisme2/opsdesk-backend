package com.opsdesk.ai.service.impl;

import com.opsdesk.ai.config.AiServiceProperties;
import com.opsdesk.ai.dto.KnowledgeChatRequest;
import com.opsdesk.ai.security.ServiceJwtTokenIssuer;
import com.opsdesk.ai.service.AiKnowledgeChatService;
import com.opsdesk.ai.vo.KnowledgeChatResponseVO;
import com.opsdesk.common.exception.BusinessException;
import com.opsdesk.common.exception.ErrorCode;
import com.opsdesk.common.security.CurrentUser;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;

/** 主应用 RAG JSON 代理实现，前端不会直接访问独立 AI 服务。 */
@Service
public class AiKnowledgeChatServiceImpl implements AiKnowledgeChatService {
    private final ServiceJwtTokenIssuer tokenIssuer;
    private final RestClient restClient;
    public AiKnowledgeChatServiceImpl(ServiceJwtTokenIssuer tokenIssuer, RestClient.Builder builder, AiServiceProperties properties) {
        this.tokenIssuer = tokenIssuer;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(properties.getConnectTimeoutSeconds()));
        factory.setReadTimeout(Duration.ofSeconds(120));
        this.restClient = builder.baseUrl(properties.getBaseUrl()).requestFactory(factory).build();
    }
    @Override public KnowledgeChatResponseVO chat(KnowledgeChatRequest request, CurrentUser currentUser) {
        try {
            InternalResponse<KnowledgeChatResponseVO> response = restClient.post().uri("/internal/rag/knowledge/chat")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenIssuer.issue(currentUser)).body(request).retrieve()
                    .body(new ParameterizedTypeReference<>() { });
            if (response == null || response.code() != 200 || response.data() == null) {
                throw new BusinessException(ErrorCode.AI_SERVICE_UNAVAILABLE, "AI 问答服务响应无效");
            }
            return response.data();
        } catch (BusinessException exception) { throw exception; }
        catch (RestClientException exception) { throw new BusinessException(ErrorCode.AI_SERVICE_UNAVAILABLE, "AI 问答服务当前不可用"); }
    }
    private record InternalResponse<T>(int code, String message, T data) { }
}
