package com.opsdesk.ai.service.impl;

import com.opsdesk.ai.config.AiServiceProperties;
import com.opsdesk.ai.dto.KnowledgeChatRequest;
import com.opsdesk.ai.security.ServiceJwtTokenIssuer;
import com.opsdesk.ai.service.AiKnowledgeChatService;
import com.opsdesk.ai.vo.KnowledgeChatResponseVO;
import com.opsdesk.common.exception.BusinessException;
import com.opsdesk.common.exception.ErrorCode;
import com.opsdesk.common.security.CurrentUser;
import com.opsdesk.common.trace.TraceIdConstants;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.MDC;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.io.InputStream;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

/** 主应用 RAG JSON 代理实现，前端不会直接访问独立 AI 服务。 */
@Service
public class AiKnowledgeChatServiceImpl implements AiKnowledgeChatService {
    private final ServiceJwtTokenIssuer tokenIssuer;
    private final RestClient restClient;
    private final HttpClient streamingClient;
    private final ObjectMapper objectMapper;
    private final AiServiceProperties properties;
    public AiKnowledgeChatServiceImpl(ServiceJwtTokenIssuer tokenIssuer, RestClient.Builder builder,
                                      AiServiceProperties properties, ObjectMapper objectMapper) {
        this.tokenIssuer = tokenIssuer;
        this.properties = properties;
        this.objectMapper = objectMapper;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(properties.getConnectTimeoutSeconds()));
        factory.setReadTimeout(Duration.ofSeconds(120));
        this.restClient = builder.baseUrl(properties.getBaseUrl()).requestFactory(factory).build();
        this.streamingClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(properties.getConnectTimeoutSeconds())).build();
    }

    @Override public StreamingResponseBody stream(KnowledgeChatRequest request, CurrentUser currentUser) {
        try {
            String base = properties.getBaseUrl().endsWith("/")
                    ? properties.getBaseUrl().substring(0, properties.getBaseUrl().length() - 1) : properties.getBaseUrl();
            HttpRequest upstream = HttpRequest.newBuilder(URI.create(base + "/internal/rag/knowledge/chat/stream"))
                    .timeout(Duration.ofSeconds(120))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenIssuer.issue(currentUser))
                    .header(HttpHeaders.CONTENT_TYPE, "application/json")
                    .header(TraceIdConstants.TRACE_ID_HEADER, currentTraceId())
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(request))).build();
            HttpResponse<InputStream> response = streamingClient.send(upstream, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() / 100 != 2) {
                response.body().close();
                throw new BusinessException(ErrorCode.AI_SERVICE_UNAVAILABLE, "AI 流式问答服务当前不可用");
            }
            return outputStream -> {
                try (InputStream input = response.body()) { input.transferTo(outputStream); outputStream.flush(); }
            };
        } catch (BusinessException exception) { throw exception; }
        catch (Exception exception) { throw new BusinessException(ErrorCode.AI_SERVICE_UNAVAILABLE, "AI 流式问答服务当前不可用"); }
    }
    @Override public KnowledgeChatResponseVO chat(KnowledgeChatRequest request, CurrentUser currentUser) {
        try {
            InternalResponse<KnowledgeChatResponseVO> response = restClient.post().uri("/internal/rag/knowledge/chat")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenIssuer.issue(currentUser))
                    .header(TraceIdConstants.TRACE_ID_HEADER, currentTraceId()).body(request).retrieve()
                    .body(new ParameterizedTypeReference<>() { });
            if (response == null || response.code() != 200 || response.data() == null) {
                throw new BusinessException(ErrorCode.AI_SERVICE_UNAVAILABLE, "AI 问答服务响应无效");
            }
            return response.data();
        } catch (BusinessException exception) { throw exception; }
        catch (RestClientException exception) { throw new BusinessException(ErrorCode.AI_SERVICE_UNAVAILABLE, "AI 问答服务当前不可用"); }
    }
    /** 下游审计沿用当前请求 TraceId；异步输出只透传字节，不再读取 MDC。 */
    private String currentTraceId() {
        String traceId = MDC.get(TraceIdConstants.TRACE_ID_MDC_KEY);
        return traceId == null ? "" : traceId;
    }
    private record InternalResponse<T>(int code, String message, T data) { }
}
