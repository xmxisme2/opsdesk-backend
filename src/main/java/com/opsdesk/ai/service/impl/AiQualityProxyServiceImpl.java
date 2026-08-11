package com.opsdesk.ai.service.impl;

import com.opsdesk.ai.config.AiServiceProperties;
import com.opsdesk.ai.dto.AiQualityRangeRequest;
import com.opsdesk.ai.dto.AiQualitySampleSearchRequest;
import com.opsdesk.ai.security.ServiceJwtTokenIssuer;
import com.opsdesk.ai.service.AiQualityProxyService;
import com.opsdesk.ai.vo.AiQualityOverviewVO;
import com.opsdesk.ai.vo.AiQualitySampleVO;
import com.opsdesk.common.exception.BusinessException;
import com.opsdesk.common.exception.ErrorCode;
import com.opsdesk.common.response.PageResult;
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
 * AI 质量统计代理实现。
 *
 * <p>主应用完成 ADMIN 鉴权后签发短期 Service JWT，独立 AI 服务再次校验角色快照并执行聚合查询。</p>
 */
@Service
public class AiQualityProxyServiceImpl implements AiQualityProxyService {
    private final ServiceJwtTokenIssuer tokenIssuer;
    private final RestClient restClient;

    public AiQualityProxyServiceImpl(ServiceJwtTokenIssuer tokenIssuer, RestClient.Builder restClientBuilder,
                                     AiServiceProperties properties) {
        this.tokenIssuer = tokenIssuer;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(properties.getConnectTimeoutSeconds()));
        factory.setReadTimeout(Duration.ofSeconds(properties.getReadTimeoutSeconds()));
        this.restClient = restClientBuilder.baseUrl(properties.getBaseUrl()).requestFactory(factory).build();
    }

    @Override
    public AiQualityOverviewVO overview(AiQualityRangeRequest request, CurrentUser currentUser) {
        AiQualityRangeRequest body = request == null ? new AiQualityRangeRequest(null, null) : request;
        return post("/internal/admin/quality/overview", body, currentUser, new ParameterizedTypeReference<>() { });
    }

    @Override
    public PageResult<AiQualitySampleVO> searchSamples(AiQualitySampleSearchRequest request, CurrentUser currentUser) {
        return post("/internal/admin/quality/samples/search", request, currentUser, new ParameterizedTypeReference<>() { });
    }

    private <T> T post(String path, Object body, CurrentUser currentUser, ParameterizedTypeReference<InternalApiResponse<T>> type) {
        try {
            InternalApiResponse<T> response = restClient.post().uri(path)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenIssuer.issue(currentUser))
                    .header(TraceIdConstants.TRACE_ID_HEADER, currentTraceId())
                    .body(body).retrieve().body(type);
            if (response == null || response.code() != 200 || response.data() == null) {
                throw new BusinessException(ErrorCode.AI_SERVICE_UNAVAILABLE, "AI 质量统计响应无效");
            }
            return response.data();
        } catch (BusinessException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new BusinessException(ErrorCode.AI_SERVICE_UNAVAILABLE, "AI 质量统计服务当前不可用");
        }
    }

    private String currentTraceId() {
        String traceId = MDC.get(TraceIdConstants.TRACE_ID_MDC_KEY);
        return traceId == null ? "" : traceId;
    }

    private record InternalApiResponse<T>(int code, String message, T data) { }
}
