package com.c2c.csm.infrastructure.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.c2c.csm.common.util.CommonMapper;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

@Configuration
public class RabbitMqConfig {
    
    @Bean
    public Declarables commandMqDeclarables(
            @Value("${c2c.mq.command.exchange}") String exchangeName,
            @Value("${c2c.mq.command.queue}") String queueName,
            @Value("${c2c.mq.command.routing-key}") String routingKey
    ) {
        TopicExchange exchange = new TopicExchange(exchangeName, true, false);
        Queue queue = new Queue(queueName, true, false, false);
        Binding binding = BindingBuilder.bind(queue).to(exchange).with(routingKey);
        return new Declarables(exchange, queue, binding);
    }

    @Bean
    public Declarables ackMqDeclarables(
            @Value("${c2c.mq.ack.exchange}") String exchangeName,
            @Value("${c2c.mq.ack.queue}") String queueName,
            @Value("${c2c.mq.ack.routing-key}") String routingKey
    ) {
        TopicExchange exchange = new TopicExchange(exchangeName, true, false);
        Queue queue = new Queue(queueName, true, false, false);
        Binding binding = BindingBuilder.bind(queue).to(exchange).with(routingKey);
        return new Declarables(exchange, queue, binding);
    }

    @Bean
    public MessageConverter mqMessageConverter(CommonMapper commonMapper) {
        ObjectMapper objectMapper = commonMapper.rawMapper();
        JsonMapper jsonMapper = (objectMapper instanceof JsonMapper)
                ? (JsonMapper) objectMapper
                : JsonMapper.builder().build();
        JacksonJsonMessageConverter converter = new JacksonJsonMessageConverter(jsonMapper);
        converter.setAssumeSupportedContentType(true);
        return converter;
    }
    
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter mqMessageConverter,
            ThreadPoolTaskExecutor rabbitListenerTaskExecutor
    ) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(mqMessageConverter);
        factory.setTaskExecutor(rabbitListenerTaskExecutor);
        factory.setAfterReceivePostProcessors((MessagePostProcessor) message -> {
            MessageProperties props = message.getMessageProperties();
            if (props.getContentType() == null) {
                props.setContentType(MessageProperties.CONTENT_TYPE_JSON);
            }
            if (props.getPriority() == null) {
                props.setPriority(0);
            }
            return message;
        });
        return factory;
    }

    @Bean
    public ThreadPoolTaskExecutor rabbitListenerTaskExecutor(
            @Value("${c2c.mq.listener.core-pool-size:2}") int corePoolSize,
            @Value("${c2c.mq.listener.max-pool-size:8}") int maxPoolSize
    ) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setThreadNamePrefix("mq-event-");
        executor.initialize();
        return executor;
    }
    
}
