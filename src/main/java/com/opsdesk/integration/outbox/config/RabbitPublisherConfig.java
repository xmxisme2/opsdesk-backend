package com.opsdesk.integration.outbox.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 主应用 RabbitMQ 发布配置。
 */
@Configuration
public class RabbitPublisherConfig {

    @Bean
    public Jackson2JsonMessageConverter rabbitJsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public RabbitTemplate.ConfirmCallback outboxConfirmCallback() {
        return (correlationData, acknowledged, cause) -> {
            // Outbox 发布器通过同步 publisher confirm 判定结果，本回调只保留扩展入口。
        };
    }
}
