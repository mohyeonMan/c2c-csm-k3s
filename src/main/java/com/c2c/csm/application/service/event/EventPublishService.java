package com.c2c.csm.application.service.event;

import org.springframework.stereotype.Service;

import com.c2c.csm.application.model.Event;
import com.c2c.csm.application.port.out.event.EventPublishUsecase;
import com.c2c.csm.application.port.out.event.PublishEventPort;
import com.c2c.csm.infrastructure.registry.EventRegistry;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventPublishService implements EventPublishUsecase {
    private final PublishEventPort publishEventPort;
    private final EventRegistry eventRegistry;

    @Override
    public void saveAndPublish(String routingKey, Event event) {
        log.info(
            "event: save start eventId={}, userId={}, type={}, action={}, status={}",
            event.getEventId(),
            event.getUserId(),
            event.getType(),
            event.getAction(),
            event.getStatus()
        );
        eventRegistry.save(event);
        log.info("event: save success eventId={}", event.getEventId());

        if(routingKey == null || routingKey.isEmpty()) {
            log.warn("No routing key provided for event: {}", event);
            return;
        }
        
        log.info("event: publish start eventId={}, routingKey={}", event.getEventId(), routingKey);
        publishEventPort.publishEvent(routingKey, event);
        log.info("event: publish success eventId={}, routingKey={}", event.getEventId(), routingKey);
    }
    
}
