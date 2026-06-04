package com.opsdesk.common.event;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * 本地领域事件发布器。
 *
 * <p>用于在不接入 RabbitMQ 的一期阶段完成通知、审计、统计等事件解耦。</p>
 */
@Component
public class LocalDomainEventPublisher implements DomainEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    public LocalDomainEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    public void publish(Object event) {
        applicationEventPublisher.publishEvent(event);
    }
}

