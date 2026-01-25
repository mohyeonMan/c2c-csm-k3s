package com.c2c.csm.application.port.in.mq.command;

import java.time.Instant;

import com.c2c.csm.application.model.Action;
import com.c2c.csm.application.model.Command;
import com.c2c.csm.application.model.Event;
import com.c2c.csm.application.model.EventType;
import com.c2c.csm.application.model.Status;
import com.c2c.csm.application.port.out.mq.EventPublishUsecase;
import com.c2c.csm.application.port.out.presenece.SessionPresencePort;
import com.c2c.csm.common.util.CommonMapper;
import com.c2c.csm.common.util.IdGenerator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public abstract class AbstractCommandHandler implements CommandHandler {
    private final EventPublishUsecase eventPublishUsecase;
    private final SessionPresencePort sessionPresencePort;
    private final CommonMapper commonMapper;

    @Override
    public void handle(Command command) {
        try {
            Object resultPayload = doHandle(command);
            if (resultPayload != null) {
                sendResult(command, resultPayload);
            }
        } catch (Exception ex) {
            log.error("Error handling command: {}", command, ex);
            sendErrorResult(command, ex);
        }
    }

    protected abstract Object doHandle(Command command);

    protected void sendEvent(Event event){
        String routingKey = sessionPresencePort.getRoutingKeyByUserId(event.getUserId());
        eventPublishUsecase.saveAndPublish(routingKey, event);
    }

    protected Event buildEvent(
        Command command,
        String userId,
        EventType type,
        Action action,
        Object payload,
        Status status
    ){
        Event event = Event.builder()
            .requestId(command.getRequestId())
            .commandId(command.getCommandId())
            .userId(userId)
            .eventId(IdGenerator.generateId("evt"))
            .type(type)
            .action(action)
            .payload(commonMapper.write(payload))
            .status(status)
            .sentAt(Instant.now())
            .build();

        return event;

    }

    protected void sendResult(Command command, Object payload){
        Event result = buildResult(command, Status.SUCCESS, payload);
        sendEvent(result);
    }

    protected void sendErrorResult(Command command, Exception ex){
        String errorPayload = null;
        Event result = buildResult(command, Status.ERROR, errorPayload);
        sendEvent(result);
    }   

    protected Event buildResult(
        Command command,
        Status status,
        Object payload
    ){
        Event event = buildEvent(
            command,
            command.getUserId(),
            EventType.RESULT,
            command.getAction(),
            payload,
            status
        );

        return event;
    }
}
