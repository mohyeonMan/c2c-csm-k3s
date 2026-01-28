package com.c2c.csm.application.service.ack;

import org.springframework.stereotype.Service;

import com.c2c.csm.application.model.Ack;
import com.c2c.csm.application.port.in.mq.ack.AcknowledgeUseCase;
import com.c2c.csm.infrastructure.registry.EventRegistry;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AcknowledgeService implements AcknowledgeUseCase{
    private final EventRegistry registry;

    @Override
    public void acknowledgeEvent(Ack ack) {
        log.info(
            "ack: start ackId={}, eventId={}, sentAt={}",
            ack.getAckId(),
            ack.getEventId(),
            ack.getSentAt()
        );
        

        String eventId = ack.getEventId();
        log.info("ack: remove eventId={}", eventId);

        registry.remove(eventId);
        log.info("ack: success eventId={}", eventId);
        
    }

    
    
}
