package com.opsdesk.ai.service.impl;

import com.opsdesk.ai.config.AiServiceProperties;
import com.opsdesk.ai.dto.IndexRebuildRequest;
import com.opsdesk.ai.dto.IndexReindexRequest;
import com.opsdesk.ai.dto.IndexReconcileRequest;
import com.opsdesk.ai.security.ServiceJwtTokenIssuer;
import com.opsdesk.ai.service.AiIndexAdminService;
import com.opsdesk.ai.vo.IndexTaskAcceptedVO;
import com.opsdesk.common.exception.BusinessException;
import com.opsdesk.common.exception.ErrorCode;
import com.opsdesk.common.security.CurrentUser;
import com.opsdesk.common.trace.TraceIdConstants;
import com.opsdesk.common.util.IdParser;
import com.opsdesk.integration.knowledge.KnowledgeOutboxEventService;
import com.opsdesk.knowledge.mapper.KnowledgeArticleMapper;
import com.opsdesk.knowledge.mapper.KnowledgeArticleRow;
import org.slf4j.MDC;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;

/**
 * 索引管理服务实现。
 *
 * <p>单篇重建写入主库 Outbox；全量重建由独立 AI 服务异步创建新索引并切换别名。</p>
 */
@Service
public class AiIndexAdminServiceImpl implements AiIndexAdminService {
    private final KnowledgeArticleMapper articleMapper;
    private final KnowledgeOutboxEventService eventService;
    private final ServiceJwtTokenIssuer tokenIssuer;
    private final RestClient restClient;

    public AiIndexAdminServiceImpl(KnowledgeArticleMapper articleMapper,
                                   KnowledgeOutboxEventService eventService,
                                   ServiceJwtTokenIssuer tokenIssuer,
                                   RestClient.Builder restClientBuilder,
                                   AiServiceProperties properties) {
        this.articleMapper = articleMapper;
        this.eventService = eventService;
        this.tokenIssuer = tokenIssuer;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(properties.getConnectTimeoutSeconds()));
        factory.setReadTimeout(Duration.ofSeconds(properties.getReadTimeoutSeconds()));
        this.restClient = restClientBuilder.baseUrl(properties.getBaseUrl()).requestFactory(factory).build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public IndexTaskAcceptedVO reindex(String articleId, IndexReindexRequest request, CurrentUser currentUser) {
        Long id = IdParser.parseRequired(articleId, "知识文章ID");
        KnowledgeArticleRow article = articleMapper.findById(id);
        if (article == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "知识文章不存在");
        }
        if (!"PUBLISHED".equals(article.getStatus())) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT, "只有已发布文章可以重建索引");
        }
        String taskId = eventService.record(
                "KnowledgeArticleReindexRequested",
                "knowledge.article.reindex-requested",
                article,
                article.getVersion() == null ? 1L : article.getVersion(),
                article.getStatus(),
                currentUser.getUserId()
        );
        return new IndexTaskAcceptedVO(taskId);
    }

    @Override
    public IndexTaskAcceptedVO rebuild(IndexRebuildRequest request, CurrentUser currentUser) {
        return postIndexTask("/internal/admin/index/rebuild", request, currentUser);
    }

    @Override
    public IndexTaskAcceptedVO reconcile(IndexReconcileRequest request, CurrentUser currentUser) {
        return postIndexTask("/internal/admin/index/reconcile", request, currentUser);
    }

    private IndexTaskAcceptedVO postIndexTask(String path, Object request, CurrentUser currentUser) {
        try {
            InternalApiResponse<IndexTaskAcceptedVO> response = restClient.post()
                    .uri(path)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenIssuer.issue(currentUser))
                    .header(TraceIdConstants.TRACE_ID_HEADER, currentTraceId())
                    .body(request)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() { });
            if (response == null || response.code() != 200 || response.data() == null) {
                throw new BusinessException(ErrorCode.AI_SERVICE_UNAVAILABLE, "AI 索引任务响应无效");
            }
            return response.data();
        } catch (BusinessException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new BusinessException(ErrorCode.AI_SERVICE_UNAVAILABLE, "AI 索引服务当前不可用");
        }
    }

    private String currentTraceId() {
        String traceId = MDC.get(TraceIdConstants.TRACE_ID_MDC_KEY);
        return traceId == null ? "" : traceId;
    }

    private record InternalApiResponse<T>(int code, String message, T data) {
    }
}
