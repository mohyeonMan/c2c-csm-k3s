package com.c2c.csm.application.service.command;

import java.util.Map;

import org.springframework.stereotype.Service;

import com.c2c.csm.application.model.Action;
import com.c2c.csm.application.model.Command;
import com.c2c.csm.application.model.Event;
import com.c2c.csm.application.model.EventType;
import com.c2c.csm.application.model.Status;
import com.c2c.csm.application.port.out.event.EventPublishUsecase;
import com.c2c.csm.application.port.out.presence.SessionPresencePort;
import com.c2c.csm.common.exception.C2cException;
import com.c2c.csm.common.exception.ErrorCode;
import com.c2c.csm.common.util.CommonMapper;

import com.c2c.csm.application.service.metric.MetricsService;
import com.c2c.csm.infrastructure.registry.RoomRegistry;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ClientMessageCommandHandler extends AbstractCommandHandler {
    private final RoomRegistry roomRegistry;

    public ClientMessageCommandHandler(
        EventPublishUsecase eventPublishUsecase,
        SessionPresencePort sessionPresencePort,
        CommonMapper commonMapper,
        MetricsService metricsService,
        RoomRegistry roomRegistry
    ) {
        super(eventPublishUsecase, sessionPresencePort, commonMapper, metricsService);
        this.roomRegistry = roomRegistry;
    }

    @Override
    public Action supports() {
        return Action.CLIENT_MESSAGE;
    }

    public record ClientMessagePayload(String roomId, String message) {}

    @Override
    protected Object doHandle(Command command) {
        ClientMessagePayload payload = parsePayload(command.getPayload(), ClientMessagePayload.class);
        String userId = command.getUserId();
        String roomId = payload == null ? null : payload.roomId();
        String message = payload == null ? null : payload.message();
        int messageLength = message == null ? 0 : message.length();
        log.info(
            "command: client message start userId={}, roomId={}, messageLength={}",
            userId,
            roomId,
            messageLength
        );

        String nickname = roomRegistry.findMemberNickname(roomId, userId)
            .orElseThrow(() -> new C2cException(ErrorCode.CSM_NICKNAME_NOT_FOUND));

        Object messagePayload = Map.of(
            "roomId", roomId,
            "userId", userId,
            "message", message,
            "nickname", nickname
        );

        roomRegistry.findOnlineMembers(roomId).forEach(targetUserId -> {
            if (targetUserId.equals(userId)) return;
            Event event = buildEvent(command, targetUserId, EventType.MESSAGE, Action.CLIENT_MESSAGE, messagePayload, Status.SUCCESS);
            sendEvent(event);
        });

        log.info("command: client message success userId={}, roomId={}", userId, roomId);
        return messagePayload;
    }
}
