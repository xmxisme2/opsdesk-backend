package com.opsdesk.integration.outbox.service;

import com.opsdesk.integration.knowledge.KnowledgeOutboxEventService;
import com.opsdesk.integration.outbox.config.AiEventProperties;
import com.opsdesk.integration.outbox.entity.EventOutbox;
import com.opsdesk.integration.outbox.mapper.EventOutboxMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Outbox RabbitMQ 发布器。
 *
 * <p>先原子抢占事件，再使用 publisher confirm 确认投递；失败按指数退避重试。</p>
 */
@Service
@Slf4j
@ConditionalOnProperty(prefix = "opsdesk.ai-events", name = "enabled", havingValue = "true")
public class OutboxPublisher {

    /** 单条消息 publisher confirm 等待上限。 */
    private static final long CONFIRM_TIMEOUT_MILLIS = 5000L;
    /** 失败退避最大秒数，避免中间件故障时高频重试。 */
    private static final long MAX_RETRY_DELAY_SECONDS = 300L;
    /** 保存到数据库的错误摘要最大长度。 */
    private static final int MAX_ERROR_LENGTH = 1000;

    private final EventOutboxMapper outboxMapper;
    private final RabbitTemplate rabbitTemplate;
    private final AiEventProperties properties;

    public OutboxPublisher(EventOutboxMapper outboxMapper,
                           RabbitTemplate rabbitTemplate,
                           AiEventProperties properties) {
        this.outboxMapper = outboxMapper;
        this.rabbitTemplate = rabbitTemplate;
        this.properties = properties;
    }

    @Scheduled(fixedDelayString = "${opsdesk.ai-events.dispatch-delay-ms:1000}")
    public void dispatch() {
        outboxMapper.recoverStaleSending();
        int batchSize = Math.min(Math.max(1, properties.getBatchSize()), 200);
        List<EventOutbox> events = outboxMapper.findDispatchable(batchSize);
        events.forEach(this::publishOne);
    }

    private void publishOne(EventOutbox event) {
        if (outboxMapper.claim(event.getId()) != 1) {
            return;
        }
        try {
            Boolean confirmed = rabbitTemplate.invoke(operations -> {
                operations.send(
                        KnowledgeOutboxEventService.EXCHANGE,
                        event.getRoutingKey(),
                        MessageBuilder.withBody(event.getPayload().getBytes(java.nio.charset.StandardCharsets.UTF_8))
                                .setMessageId(event.getEventId())
                                .setContentType("application/json")
                                .setDeliveryMode(org.springframework.amqp.core.MessageDeliveryMode.PERSISTENT)
                                .build()
                );
                return operations.waitForConfirms(CONFIRM_TIMEOUT_MILLIS);
            });
            if (!Boolean.TRUE.equals(confirmed)) {
                throw new IllegalStateException("RabbitMQ publisher confirm 未确认");
            }
            outboxMapper.markPublished(event.getId());
        } catch (Exception exception) {
            long retryDelay = retryDelaySeconds(event.getRetryCount());
            outboxMapper.markFailed(event.getId(), errorSummary(exception), retryDelay);
            log.warn("知识事件发布失败 eventId={}, retryDelay={}s", event.getEventId(), retryDelay);
        }
    }

    private long retryDelaySeconds(Integer retryCount) {
        int count = retryCount == null ? 0 : Math.min(retryCount, 8);
        return Math.min(1L << count, MAX_RETRY_DELAY_SECONDS);
    }

    private String errorSummary(Exception exception) {
        String message = exception.getClass().getSimpleName() + ": " + exception.getMessage();
        return message.length() <= MAX_ERROR_LENGTH ? message : message.substring(0, MAX_ERROR_LENGTH);
    }
}
