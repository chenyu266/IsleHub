package com.islehub.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String EXCHANGE = "islehub.order.topic";
    public static final String QUEUE_STOCK_CONFIRM = "islehub.stock.confirm";
    public static final String ROUTING_STOCK_CONFIRM = "order.stock.confirm";

    @Bean
    public TopicExchange orderTopicExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    public Queue stockConfirmQueue() {
        return QueueBuilder.durable(QUEUE_STOCK_CONFIRM).build();
    }

    @Bean
    public Binding stockConfirmBinding() {
        return BindingBuilder.bind(stockConfirmQueue())
                .to(orderTopicExchange())
                .with(ROUTING_STOCK_CONFIRM);
    }

    /**
     * JSON 消息转换器，替代默认的 Java 序列化。
     */
    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        DefaultJackson2JavaTypeMapper typeMapper = new DefaultJackson2JavaTypeMapper();
        typeMapper.setTrustedPackages("com.islehub.common.mq");
        converter.setJavaTypeMapper(typeMapper);
        return converter;
    }
}
