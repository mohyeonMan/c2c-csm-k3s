package com.c2c.csm.application.service.command;

import java.util.Map;

import org.springframework.stereotype.Service;

import com.c2c.csm.application.model.Action;
import com.c2c.csm.application.model.Command;
import com.c2c.csm.application.model.Event;
import com.c2c.csm.application.model.EventType;
import com.c2c.csm.application.model.Status;
import com.c2c.csm.application.port.out.event.EventPublishUsecase;
import com.c2c.csm.application.port.out.presenece.SessionPresencePort;
import com.c2c.csm.common.util.CommonMapper;
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
        RoomRegistry roomRegistry
    ) {
        super(eventPublishUsecase, sessionPresencePort, commonMapper);
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
        String roomId = payload.roomId();
        String message = payload.message();

        String nickname = roomRegistry.findMemberNickname(roomId, userId)
            .orElseThrow(() -> new RuntimeException("닉네임을 찾을 수 없음"));

        Object messagePayload = Map.of(
            "roomId", roomId,
            "userId", userId,
            "message", message,
            "nickname", nickname
        );

        roomRegistry.findMembers(roomId).forEach(targetUserId -> {
            if(targetUserId.equals(userId)) return;
            Event event = buildEvent(command, targetUserId, EventType.MESSAGE, Action.CLIENT_MESSAGE, messagePayload, Status.SUCCESS);
            sendEvent(event);
        });

        return messagePayload;
    }
}
