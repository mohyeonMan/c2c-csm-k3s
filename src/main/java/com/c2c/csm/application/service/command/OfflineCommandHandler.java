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
import com.c2c.csm.application.service.room.RoomRegistryService;
import com.c2c.csm.application.service.room.RoomRegistryService.PresenceResult;
import com.c2c.csm.common.util.CommonMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class OfflineCommandHandler extends AbstractCommandHandler {
    private final RoomRegistryService roomRegistryService;

    public OfflineCommandHandler(
        EventPublishUsecase eventPublishUsecase,
        SessionPresencePort sessionPresencePort,
        CommonMapper commonMapper,
        RoomRegistryService roomRegistryService
    ) {
        super(eventPublishUsecase, sessionPresencePort, commonMapper);
        this.roomRegistryService = roomRegistryService;
    }

    @Override
    public Action supports() {
        return Action.OFFLINE;
    }

    public record OfflinePayload(String roomId) {}

    @Override
    protected Object doHandle(Command command) {
        OfflinePayload payload = parsePayload(command.getPayload(), OfflinePayload.class);
        String userId = command.getUserId();
        String roomId = payload.roomId();
        log.info("command: offline start userId={}, roomId={}", userId, roomId);

        PresenceResult presenceResult = roomRegistryService.markOffline(roomId, userId);
        Map<String, Object> notifyPayload = presenceResult.notifyPayload();
        presenceResult.onlineMembers().forEach(targetUserId -> {
            Event event = buildEvent(command, targetUserId, EventType.NOTIFY, Action.OFFLINE, notifyPayload, Status.SUCCESS);
            sendEvent(event);
        });

        Map<String, Object> resultPayload = Map.of(
            "roomId", roomId
        );
        log.info("command: offline success userId={}, roomId={}", userId, roomId);
        return resultPayload;
    }
}
