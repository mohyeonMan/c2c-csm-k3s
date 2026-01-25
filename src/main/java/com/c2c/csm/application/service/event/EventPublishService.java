package com.c2c.csm.application.service.event;

import org.springframework.stereotype.Service;

import com.c2c.csm.application.model.Event;
import com.c2c.csm.application.port.out.mq.EventPublishUsecase;
import com.c2c.csm.application.port.out.mq.PublishEventPort;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventPublishService implements EventPublishUsecase {
    private final PublishEventPort publishEventPort;
    //event registry에 저장.

    @Override
    public void saveAndPublish(String routingKey, Event event) {
        //registry 저장.
        
        if(routingKey == null || routingKey.isEmpty()) {
            log.warn("No routing key provided for event: {}", event);
            return;
        }
        publishEventPort.publishEvent(routingKey, event);
    }
    
}
