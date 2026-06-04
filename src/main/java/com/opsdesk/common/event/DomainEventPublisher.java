package com.opsdesk.common.event;

/**
 * 领域事件发布接口。
 *
 * <p>首版使用本地同步事件实现，后续接入 RabbitMQ 时只替换实现，不影响业务模块调用方式。</p>
 */
public interface DomainEventPublisher {

    void publish(Object event);
}

