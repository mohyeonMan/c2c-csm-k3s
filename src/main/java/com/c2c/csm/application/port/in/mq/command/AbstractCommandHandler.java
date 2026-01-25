package com.c2c.csm.application.port.in.mq.command;

import java.time.Instant;

import com.c2c.csm.application.model.Command;
import com.c2c.csm.application.model.Event;
import com.c2c.csm.application.model.EventType;
import com.c2c.csm.application.model.Status;
import com.c2c.csm.application.port.out.mq.PublishEventPort;
import com.c2c.csm.application.port.out.presenece.SessionPresencePort;
import com.c2c.csm.common.util.CommonMapper;
import com.c2c.csm.common.util.IdGenerator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public abstract class AbstractCommandHandler implements CommandHandler {
    private final PublishEventPort publishEventPort;
    private final SessionPresencePort sessionPresencePort;
    private final CommonMapper commonMapper;

    @Override
    public void handle(Command command) {

    }

    protected abstract void doHandle(Command command);

    protected void sendResult(Command command, Object payload){
        String routingKey = sessionPresencePort.getRoutingKeyByUserId(command.getUserId());
        Event result = buildResult(command, Status.SUCCESS, payload);
        publishEventPort.publishEvent(routingKey, result);
    }

    protected void sendErrorResult(Command command, Exception ex){
        String errorPayload = null;
        String routingKey = sessionPresencePort.getRoutingKeyByUserId(command.getUserId());
        Event result = buildResult(command, Status.ERROR, errorPayload);
        publishEventPort.publishEvent(routingKey, result);
    }

    protected Event buildResult(
        Command command,
        Status status,
        Object payload
    ){
        Event event = Event.builder()
            .requestId(command.getRequestId())
            .commandId(command.getCommandId())
            .userId(command.getUserId())
            .eventId(IdGenerator.generateId("evt"))
            .type(EventType.RESULT)
            .action(command.getAction())
            .payload(commonMapper.write(payload))
            .status(status)
            .sentAt(Instant.now())
            .build();

        return event;
    }
}
