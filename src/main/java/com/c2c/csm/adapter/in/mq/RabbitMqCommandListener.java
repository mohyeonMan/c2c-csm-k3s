package com.c2c.csm.adapter.in.mq;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.c2c.csm.adapter.in.mq.dto.CommandDto;
import com.c2c.csm.application.model.Action;
import com.c2c.csm.application.model.Command;
import com.c2c.csm.application.port.in.mq.command.CommandDispatcherUseCase;
import com.c2c.csm.application.port.in.mq.command.ConsumeCommandPort;
import com.c2c.csm.common.util.TimeFormat;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class RabbitMqCommandListener implements ConsumeCommandPort {

    private final CommandDispatcherUseCase commandDispatcherUseCase;


    @Override
    @RabbitListener(queues = "${c2c.mq.command.queue}")
    public void onCommand(CommandDto commandDto) {
        log.info("Consuming command: {}", commandDto);
        Command command = Command.builder()
                .commandId(commandDto.getCommandId())
                .requestId(commandDto.getRequestId())
                .userId(commandDto.getUserId())
                .action(Action.from(commandDto.getAction()))
                .payload(commandDto.getPayload())
                .sentAt(TimeFormat.parse(commandDto.getSentAt()))
                .build();
        commandDispatcherUseCase.dispatchCommand(command);
    }

}