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
import com.c2c.csm.application.service.room.RoomRegistryService.PresenceAllResult;
import com.c2c.csm.application.service.room.RoomRegistryService.PresenceResult;
import com.c2c.csm.common.util.CommonMapper;

import com.c2c.csm.application.service.metric.MetricsService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ConnClosedCommandHandler extends AbstractCommandHandler{
    private final RoomRegistryService roomRegistryService;

    public ConnClosedCommandHandler(
        EventPublishUsecase eventPublishUsecase,
        SessionPresencePort sessionPresencePort,
        CommonMapper commonMapper,
        MetricsService metricsService,
        RoomRegistryService roomRegistryService
    ) {
        super(eventPublishUsecase, sessionPresencePort, commonMapper, metricsService);
        this.roomRegistryService = roomRegistryService;
    }

    @Override
    public Action supports() {
        return Action.CONN_CLOSED;
    }

    @Override
    protected boolean shouldSendResult(Command command) {
        return false;
    }

    @Override
    protected Object doHandle(Command command) {
        String userId = command.getUserId();
        log.info("command: conn closed start userId={}", userId);
        PresenceAllResult offlineResult = roomRegistryService.markAllRoomsOffline(userId);
        log.info("command: conn closed rooms userId={}, rooms={}", userId, offlineResult.rooms().size());

        for (PresenceResult presenceResult : offlineResult.results()) {
            Map<String, Object> notifyPayload = presenceResult.notifyPayload();
            presenceResult.onlineMembers().forEach(targetUserId -> {
                Event event = buildEvent(command, targetUserId, EventType.NOTIFY, Action.OFFLINE, notifyPayload, Status.SUCCESS);
                sendEvent(event);
            });
        }

        log.info("command: conn closed success userId={}, rooms={}", userId, offlineResult.rooms().size());
        return Map.of("rooms", offlineResult.rooms());
    }
}
