package com.c2c.csm.application.service.command;

import org.springframework.stereotype.Service;

import com.c2c.csm.application.model.Action;
import com.c2c.csm.application.model.Command;
import com.c2c.csm.application.port.out.event.EventPublishUsecase;
import com.c2c.csm.application.port.out.presenece.SessionPresencePort;
import com.c2c.csm.common.util.CommonMapper;
import com.c2c.csm.infrastructure.registry.RoomRegistry;
import com.c2c.csm.infrastructure.registry.dto.Room;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class RoomCreateCommandHandler extends AbstractCommandHandler{
    private final RoomRegistry roomRegistry;

    public RoomCreateCommandHandler(
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
        return Action.ROOM_CREATE;
    }

    public record RoomCreatePayload(String nickName) {}

    @Override
    protected Object doHandle(Command command) {
        RoomCreatePayload payload = parsePayload(command.getPayload(), RoomCreatePayload.class);
        String userId = command.getUserId();
        String nickName = payload == null ? null : payload.nickName();

        if (nickName == null || nickName.isBlank()) {
            throw new RuntimeException("nickname required");
        }

        log.info("command: room create start userId={}", userId);
        Room room = roomRegistry.createRoom(userId)
            .orElseThrow(() -> new RuntimeException("room create failed"));
        log.info("command: room create success userId={}, roomId={}", userId, room.getRoomId());

        boolean joined = roomRegistry.addMemberWithNickname(room.getRoomId(), userId, nickName);
        if (!joined) {
            roomRegistry.deleteRoom(room.getRoomId());
            throw new RuntimeException("room join failed");
        }

        return roomRegistry.getRoomSummary(room.getRoomId())
            .orElseThrow(() -> new RuntimeException("room summary failed"));
    }

}
