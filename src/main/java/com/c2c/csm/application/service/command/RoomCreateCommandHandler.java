package com.c2c.csm.application.service.command;

import org.springframework.stereotype.Service;

import com.c2c.csm.adapter.out.mq.dto.RoomDto;
import com.c2c.csm.application.model.Action;
import com.c2c.csm.application.model.Command;
import com.c2c.csm.application.port.out.event.EventPublishUsecase;
import com.c2c.csm.application.port.out.presenece.SessionPresencePort;
import com.c2c.csm.common.util.CommonMapper;
import com.c2c.csm.common.util.TimeFormat;
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



    @Override
    protected Object doHandle(Command command) {

        String userId = command.getUserId();
        log.info("command: room create start userId={}", userId);
        Room room = roomRegistry.createRoom(userId).orElseThrow(() -> new RuntimeException("방 생성 실패"));
        log.info("command: room create success userId={}, roomId={}", userId, room.getRoomId());

        return RoomDto.builder()
                .roomId(room.getRoomId())
                .ownerId(room.getOwnerId())
                .createdAt(TimeFormat.format(room.getCreatedAt()))
                .build();
    }

}
