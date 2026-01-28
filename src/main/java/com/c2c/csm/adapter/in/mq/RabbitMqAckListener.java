package com.c2c.csm.adapter.in.mq;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.c2c.csm.adapter.in.mq.dto.AckDto;
import com.c2c.csm.application.port.in.mq.ack.ConsumeAckPort;
import com.c2c.csm.common.util.TimeFormat;
import com.c2c.csm.application.model.Ack;
import com.c2c.csm.application.port.in.mq.ack.AcknowledgeUseCase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class RabbitMqAckListener implements ConsumeAckPort {
    private final AcknowledgeUseCase acknowledgeUseCase;

    @Override
    @RabbitListener(queues = "${c2c.mq.ack.queue}")
    public void onAck(AckDto ackDto) {
        log.info("Consuming ack: {}", ackDto);

        Ack ack = Ack.builder()
                .ackId(ackDto.getAckId())
                .eventId(ackDto.getEventId())
                .sentAt(TimeFormat.parse(ackDto.getSentAt()))
                .build();

        acknowledgeUseCase.acknowledgeEvent(ack);
    }

}