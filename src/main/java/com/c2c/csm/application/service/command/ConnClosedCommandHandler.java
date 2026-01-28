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
import com.c2c.csm.application.service.room.RoomRegistryService;
import com.c2c.csm.application.service.room.RoomRegistryService.LeaveAllResult;
import com.c2c.csm.application.service.room.RoomRegistryService.LeaveResult;
import com.c2c.csm.common.util.CommonMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ConnClosedCommandHandler extends AbstractCommandHandler{
    private final RoomRegistryService roomRegistryService;

    public ConnClosedCommandHandler(
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
        return Action.CONN_CLOSED;
    }

    @Override
    protected Object doHandle(Command command) {
        String userId = command.getUserId();
        log.info("command: conn closed start userId={}", userId);
        LeaveAllResult leaveAllResult = roomRegistryService.leaveAllRoomsForDisconnect(userId);
        log.info("command: conn closed rooms userId={}, rooms={}", userId, leaveAllResult.rooms().size());

        for (LeaveResult leaveResult : leaveAllResult.results()) {
            Map<String, Object> notifyPayload = leaveResult.notifyPayload();
            leaveResult.remainingMembers().forEach(targetUserId -> {
                Event event = buildEvent(command, targetUserId, EventType.NOTIFY, Action.LEAVE, notifyPayload, Status.SUCCESS);
                sendEvent(event);
            });
        }

        log.info("command: conn closed success userId={}, rooms={}", userId, leaveAllResult.rooms().size());
        return Map.of("rooms", leaveAllResult.rooms());
    }
}
