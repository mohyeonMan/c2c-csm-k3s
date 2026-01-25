package com.c2c.csm.adapter.in.mq;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.c2c.csm.adapter.in.mq.dto.CommandDto;
import com.c2c.csm.application.port.in.mq.command.ConsumeCommandPort;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class RabbitMqCommandListener implements ConsumeCommandPort {

    @Override
    @RabbitListener(queues = "${c2c.mq.command.queue}")
    public void onCommand(CommandDto commandDto) {
        log.info("Consuming command: {}", commandDto);


    }

}