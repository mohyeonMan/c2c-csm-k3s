package com.c2c.csm.application.service.command;

import java.time.Instant;
import java.util.Map;

import com.c2c.csm.application.model.Action;
import com.c2c.csm.application.model.Command;
import com.c2c.csm.application.model.Event;
import com.c2c.csm.application.model.EventType;
import com.c2c.csm.application.model.Status;
import com.c2c.csm.application.port.in.mq.command.CommandHandler;
import com.c2c.csm.application.port.out.event.EventPublishUsecase;
import com.c2c.csm.application.port.out.presence.SessionPresencePort;
import com.c2c.csm.common.exception.C2cException;
import com.c2c.csm.common.exception.ErrorCode;
import com.c2c.csm.common.util.CommonMapper;
import com.c2c.csm.common.util.IdGenerator;
import com.c2c.csm.common.util.TimeFormat;

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
            log.info(
                "command: handle start action={}, commandId={}, requestId={}, userId={}",
                command.getAction(),
                command.getCommandId(),
                command.getRequestId(),
                command.getUserId()
            );
            Object resultPayload = doHandle(command);
            log.info(
                "command: handle success action={}, commandId={}, requestId={}, userId={}",
                command.getAction(),
                command.getCommandId(),
                command.getRequestId(),
                command.getUserId()
            );
            sendResult(command, resultPayload);
        } catch (Exception ex) {
            log.error(
                "command: handle error action={}, commandId={}, requestId={}, userId={}",
                command.getAction(),
                command.getCommandId(),
                command.getRequestId(),
                command.getUserId(),
                ex
            );
            sendErrorResult(command, ex);
        }
    }

    protected abstract Object doHandle(Command command);

    protected void sendEvent(Event event){
        log.info(
            "command: send event action={}, eventId={}, userId={}, type={}, status={}",
            event.getAction(),
            event.getEventId(),
            event.getUserId(),
            event.getType(),
            event.getStatus()
        );
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
            .payload(writePayload(payload))
            .status(status)
            .sentAt(Instant.now())
            .build();

        return event;

    }

    protected void sendResult(Command command, Object payload){
        log.info(
            "command: send result action={}, commandId={}, requestId={}, userId={}, status={}",
            command.getAction(),
            command.getCommandId(),
            command.getRequestId(),
            command.getUserId(),
            Status.SUCCESS
        );
        Event result = buildResult(command, Status.SUCCESS, payload);
        sendEvent(result);
    }

    protected void sendErrorResult(Command command, Exception ex){
        log.info(
            "command: send error result action={}, commandId={}, requestId={}, userId={}, status={}",
            command.getAction(),
            command.getCommandId(),
            command.getRequestId(),
            command.getUserId(),
            Status.ERROR
        );
        Object errorPayload;
        Map<String, Object> detailPayload = Map.of(
            "action", command.getAction(),
            "requestId", command.getRequestId(),
            "commandId", command.getCommandId(),
            "userId", command.getUserId(),
            "time", TimeFormat.format(Instant.now())
        );
        if (ex instanceof C2cException c2cEx) {
            ErrorCode errorCode = c2cEx.getErrorCode();
            String reason = c2cEx.getMessage();
            if (reason == null || reason.isBlank()) {
                reason = errorCode.getDefaultMessage();
            }
            errorPayload = Map.of(
                "code", errorCode.getCode(),
                "reason", reason,
                "detail", detailPayload
            );
        } else {
            errorPayload = Map.of(
                "code", ex.getClass(),
                "reason", ex.getMessage(),
                "detail", detailPayload
            );
        }
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

    protected <T> T parsePayload(String payloadString, Class<T> type){
        return commonMapper.read(payloadString, type);
    }

    protected String writePayload(Object payload){
        if(payload == null) return null;
        return commonMapper.write(payload);
        
    }
}
