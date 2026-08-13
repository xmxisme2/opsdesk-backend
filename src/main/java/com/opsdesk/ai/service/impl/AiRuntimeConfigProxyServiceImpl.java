package com.opsdesk.ai.service.impl;

import com.opsdesk.ai.config.AiServiceProperties;
import com.opsdesk.ai.security.ServiceJwtTokenIssuer;
import com.opsdesk.ai.service.AiRuntimeConfigProxyService;
import com.opsdesk.ai.vo.AiRuntimeConfigVO;
import com.opsdesk.common.exception.BusinessException;
import com.opsdesk.common.exception.ErrorCode;
import com.opsdesk.common.security.CurrentUser;
import com.opsdesk.common.trace.TraceIdConstants;
import org.slf4j.MDC;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;

/** AI 运行配置代理实现，统一签发短期 Service JWT 并收敛内部服务异常。 */
@Service
public class AiRuntimeConfigProxyServiceImpl implements AiRuntimeConfigProxyService {
    private final ServiceJwtTokenIssuer tokenIssuer;
    private final RestClient restClient;

    public AiRuntimeConfigProxyServiceImpl(ServiceJwtTokenIssuer tokenIssuer, RestClient.Builder builder,
                                           AiServiceProperties properties) {
        this.tokenIssuer = tokenIssuer;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(properties.getConnectTimeoutSeconds()));
        factory.setReadTimeout(Duration.ofSeconds(properties.getReadTimeoutSeconds()));
        this.restClient = builder.baseUrl(properties.getBaseUrl()).requestFactory(factory).build();
    }

    @Override
    public AiRuntimeConfigVO detail(CurrentUser currentUser) {
        return post("/internal/admin/config/detail", null, currentUser);
    }

    @Override
    public AiRuntimeConfigVO update(boolean enabled, boolean ragEnabled, CurrentUser currentUser) {
        return post("/internal/admin/config/update", new UpdatePayload(enabled, ragEnabled), currentUser);
    }

    private AiRuntimeConfigVO post(String path, Object body, CurrentUser currentUser) {
        try {
            RestClient.RequestBodySpec request = restClient.post().uri(path)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenIssuer.issue(currentUser))
                    .header(TraceIdConstants.TRACE_ID_HEADER, currentTraceId());
            if (body != null) request.body(body);
            InternalApiResponse<AiRuntimeConfigVO> response = request.retrieve()
                    .body(new ParameterizedTypeReference<>() { });
            if (response == null || response.code() != 200 || response.data() == null) {
                throw new BusinessException(ErrorCode.AI_SERVICE_UNAVAILABLE,
                        response == null || response.message() == null ? "AI 运行配置响应无效" : response.message());
            }
            return response.data();
        } catch (BusinessException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new BusinessException(ErrorCode.AI_SERVICE_UNAVAILABLE, "AI 服务当前不可用");
        }
    }

    private String currentTraceId() {
        String traceId = MDC.get(TraceIdConstants.TRACE_ID_MDC_KEY);
        return traceId == null ? "" : traceId;
    }

    /** 内部更新负载字段固定，避免外部请求携带模型密钥等未授权参数。 */
    private record UpdatePayload(boolean enabled, boolean ragEnabled) { }

    /** 独立服务统一响应的内部传输对象。 */
    private record InternalApiResponse<T>(int code, String message, T data) { }
}
