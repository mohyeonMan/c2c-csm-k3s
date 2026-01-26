package com.c2c.csm.application.service.ack;

import org.springframework.stereotype.Service;

import com.c2c.csm.adapter.in.mq.dto.AckDto;
import com.c2c.csm.application.model.Ack;
import com.c2c.csm.application.port.in.mq.ack.AcknowledgeUseCase;
import com.c2c.csm.common.util.TimeFormat;
import com.c2c.csm.infrastructure.registry.EventRegistry;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AcknowledgeService implements AcknowledgeUseCase{
    private final EventRegistry registry;
    //acknowledge 처리 로직 구현

    @Override
    public void acknowledgeEvent(AckDto ackDto) {
        
        Ack ack = Ack.builder()
                    .ackId(ackDto.getAckId())
                    .eventId(ackDto.getEventId())
                    .sentAt(TimeFormat.parse(ackDto.getSentAt()))
                    .build();

        String eventId = ack.getEventId();

        log.info("ACK 처리 : {}",eventId);
        registry.remove(eventId);
        
    }

    
    
}
