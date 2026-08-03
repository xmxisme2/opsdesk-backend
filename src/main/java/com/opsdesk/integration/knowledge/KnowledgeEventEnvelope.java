package com.opsdesk.integration.knowledge;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * 知识文章领域事件信封。
 *
 * @param eventId         全局事件 ID
 * @param eventType       事件类型
 * @param eventVersion    协议版本
 * @param source          事件来源
 * @param occurredAt      发生时间
 * @param traceId         跨服务 TraceId
 * @param aggregateType   聚合类型
 * @param aggregateId     字符串文章 ID
 * @param aggregateVersion 文章业务版本
 * @param operatorId      操作人 ID
 * @param data            最小事件数据
 */
public record KnowledgeEventEnvelope(
        String eventId,
        String eventType,
        String eventVersion,
        String source,
        OffsetDateTime occurredAt,
        String traceId,
        String aggregateType,
        String aggregateId,
        long aggregateVersion,
        String operatorId,
        Map<String, Object> data
) {
}
