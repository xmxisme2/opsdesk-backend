package com.opsdesk.integration.knowledge;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opsdesk.common.id.SnowflakeIdGenerator;
import com.opsdesk.integration.outbox.entity.EventOutbox;
import com.opsdesk.integration.outbox.mapper.EventOutboxMapper;
import com.opsdesk.knowledge.mapper.KnowledgeArticleRow;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.*;

/** 知识文章 Outbox 事件协议测试。 */
class KnowledgeOutboxEventServiceTest {

    @Test
    void shouldPersistMinimalVersionedEventEnvelope() throws Exception {
        EventOutboxMapper mapper = mock(EventOutboxMapper.class);
        SnowflakeIdGenerator idGenerator = mock(SnowflakeIdGenerator.class);
        when(idGenerator.nextId()).thenReturn(99L);
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        KnowledgeOutboxEventService service = new KnowledgeOutboxEventService(mapper, idGenerator, objectMapper);
        KnowledgeArticleRow article = new KnowledgeArticleRow();
        article.setId(10L);
        article.setTitle("VPN 排障");
        article.setContent("处理步骤");
        article.setTagNames("VPN,网络");

        service.record("KnowledgeArticlePublished", "knowledge.article.published",
                article, 3L, "PUBLISHED", 1L);

        ArgumentCaptor<EventOutbox> captor = ArgumentCaptor.forClass(EventOutbox.class);
        verify(mapper).insert(captor.capture());
        EventOutbox outbox = captor.getValue();
        JsonNode payload = objectMapper.readTree(outbox.getPayload());
        assertEquals("KnowledgeArticlePublished", payload.path("eventType").asText());
        assertEquals(3L, payload.path("aggregateVersion").asLong());
        assertEquals("10", payload.path("data").path("articleId").asText());
        assertFalse(payload.path("data").path("contentHash").asText().isBlank());
        assertEquals("PENDING", outbox.getStatus());
    }
}
