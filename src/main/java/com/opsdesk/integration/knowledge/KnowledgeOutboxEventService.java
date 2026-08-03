package com.opsdesk.integration.knowledge;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opsdesk.common.exception.BusinessException;
import com.opsdesk.common.exception.ErrorCode;
import com.opsdesk.common.id.SnowflakeIdGenerator;
import com.opsdesk.common.trace.TraceIdConstants;
import com.opsdesk.integration.outbox.entity.EventOutbox;
import com.opsdesk.integration.outbox.mapper.EventOutboxMapper;
import com.opsdesk.knowledge.mapper.KnowledgeArticleRow;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 知识文章事务事件写入服务。
 *
 * <p>调用方必须处于知识文章事务内，使文章变化与 Outbox 插入同成同败。</p>
 */
@Service
public class KnowledgeOutboxEventService {

    /** 知识文章聚合类型，不允许前端传入。 */
    public static final String AGGREGATE_TYPE = "KNOWLEDGE_ARTICLE";
    /** 事件协议首版版本。 */
    public static final String EVENT_VERSION = "1.0";
    /** RabbitMQ Topic Exchange 名称。 */
    public static final String EXCHANGE = "opsdesk.domain.events";

    private final EventOutboxMapper outboxMapper;
    private final SnowflakeIdGenerator idGenerator;
    private final ObjectMapper objectMapper;

    public KnowledgeOutboxEventService(EventOutboxMapper outboxMapper,
                                       SnowflakeIdGenerator idGenerator,
                                       ObjectMapper objectMapper) {
        this.outboxMapper = outboxMapper;
        this.idGenerator = idGenerator;
        this.objectMapper = objectMapper;
    }

    public String record(String eventType,
                       String routingKey,
                       KnowledgeArticleRow article,
                       long aggregateVersion,
                       String status,
                       Long operatorId) {
        String eventId = UUID.randomUUID().toString().replace("-", "");
        String contentHash = contentHash(article);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("articleId", String.valueOf(article.getId()));
        data.put("status", status);
        data.put("contentHash", contentHash);
        data.put("updatedAt", OffsetDateTime.now().toString());

        KnowledgeEventEnvelope envelope = new KnowledgeEventEnvelope(
                eventId,
                eventType,
                EVENT_VERSION,
                "opsdesk-backend",
                OffsetDateTime.now(),
                currentTraceId(),
                AGGREGATE_TYPE,
                String.valueOf(article.getId()),
                aggregateVersion,
                operatorId == null ? null : String.valueOf(operatorId),
                data
        );

        EventOutbox outbox = new EventOutbox();
        outbox.setId(idGenerator.nextId());
        outbox.setEventId(eventId);
        outbox.setAggregateType(AGGREGATE_TYPE);
        outbox.setAggregateId(article.getId());
        outbox.setEventType(eventType);
        outbox.setEventVersion(EVENT_VERSION);
        outbox.setRoutingKey(routingKey);
        outbox.setPayload(writePayload(envelope));
        outbox.setStatus("PENDING");
        outbox.setRetryCount(0);
        outbox.setCreateBy(operatorId);
        outbox.setUpdateBy(operatorId);
        outboxMapper.insert(outbox);
        return eventId;
    }

    public String contentHash(KnowledgeArticleRow article) {
        String source = String.join("\n",
                safe(article.getTitle()),
                safe(article.getSummary()),
                safe(article.getContent()),
                safe(article.getTagNames()));
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(source.getBytes(StandardCharsets.UTF_8))
            );
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "知识文章内容哈希生成失败");
        }
    }

    private String writePayload(KnowledgeEventEnvelope envelope) {
        try {
            return objectMapper.writeValueAsString(envelope);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "知识事件序列化失败");
        }
    }

    private String currentTraceId() {
        String traceId = MDC.get(TraceIdConstants.TRACE_ID_MDC_KEY);
        return traceId == null ? "" : traceId;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
