package com.c2c.csm.adapter.out.mq;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.c2c.csm.adapter.out.mq.dto.EventDto;
import com.c2c.csm.application.model.Event;
import com.c2c.csm.application.port.out.event.PublishEventPort;
import com.c2c.csm.common.util.TimeFormat;

import lombok.extern.slf4j.Slf4j;


@Slf4j
@Component
public class RabbitMqEventPulisher implements PublishEventPort{

    private final RabbitTemplate rabbitTemplate;
    private final String exchange;

    public RabbitMqEventPulisher(
            RabbitTemplate rabbitTemplate,
            @Value("${c2c.mq.event.exchange}") String exchange) {
        this.rabbitTemplate = rabbitTemplate;
        this.exchange = exchange;
    }

    @Override
    public void publishEvent(String routingKey, Event event) {
        log.info("event = {}", event);

        EventDto eventDto = EventDto.builder()
                .requestId(event.getRequestId())
                .commandId(event.getCommandId())
                .userId(event.getUserId())
                .eventId(event.getEventId())
                .type(event.getType().name())
                .action(event.getAction().name())
                .payload(event.getPayload())
                .status(event.getStatus().name())
                .sentAt(TimeFormat.format(event.getSentAt()))
                .build();

        rabbitTemplate.convertAndSend(exchange, routingKey, eventDto);
    }

}