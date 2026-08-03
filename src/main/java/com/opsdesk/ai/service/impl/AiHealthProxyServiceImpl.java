package com.opsdesk.ai.service.impl;

import com.opsdesk.ai.config.AiServiceProperties;
import com.opsdesk.ai.dto.AiConnectionTestRequest;
import com.opsdesk.ai.security.ServiceJwtTokenIssuer;
import com.opsdesk.ai.service.AiHealthProxyService;
import com.opsdesk.ai.vo.AiServiceHealthVO;
import com.opsdesk.ai.vo.AiConnectionTestVO;
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

/**
 * 主应用 AI 健康代理实现。
 *
 * <p>只通过短期 Service JWT 调用内部接口；远端错误统一收敛为 AI 服务不可用。</p>
 */
@Service
public class AiHealthProxyServiceImpl implements AiHealthProxyService {

    private final ServiceJwtTokenIssuer tokenIssuer;
    private final RestClient restClient;

    public AiHealthProxyServiceImpl(ServiceJwtTokenIssuer tokenIssuer,
                                    RestClient.Builder restClientBuilder,
                                    AiServiceProperties properties) {
        this.tokenIssuer = tokenIssuer;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(properties.getConnectTimeoutSeconds()));
        requestFactory.setReadTimeout(Duration.ofSeconds(properties.getReadTimeoutSeconds()));
        this.restClient = restClientBuilder
                .baseUrl(properties.getBaseUrl())
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public AiServiceHealthVO check(CurrentUser currentUser) {
        String serviceToken = tokenIssuer.issue(currentUser);
        try {
            InternalApiResponse<AiServiceHealthVO> response = restClient.post()
                    .uri("/internal/health/check")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + serviceToken)
                    .header(TraceIdConstants.TRACE_ID_HEADER, currentTraceId())
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });
            if (response == null || response.code() != 200 || response.data() == null) {
                throw new BusinessException(ErrorCode.AI_SERVICE_UNAVAILABLE, "AI 服务健康响应无效");
            }
            return response.data();
        } catch (BusinessException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new BusinessException(ErrorCode.AI_SERVICE_UNAVAILABLE, "AI 服务当前不可用");
        }
    }

    @Override
    public AiConnectionTestVO testConnection(AiConnectionTestRequest request, CurrentUser currentUser) {
        String serviceToken = tokenIssuer.issue(currentUser);
        try {
            InternalApiResponse<AiConnectionTestVO> response = restClient.post()
                    .uri("/internal/admin/model/test")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + serviceToken)
                    .header(TraceIdConstants.TRACE_ID_HEADER, currentTraceId())
                    .body(request)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() { });
            if (response == null || response.code() != 200 || response.data() == null) {
                throw new BusinessException(ErrorCode.AI_SERVICE_UNAVAILABLE, "AI 连接测试响应无效");
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

    /**
     * 仅用于解析独立服务统一响应的内部传输对象。
     */
    private record InternalApiResponse<T>(int code, String message, T data) {
    }
}
