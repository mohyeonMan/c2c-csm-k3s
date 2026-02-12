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

import com.c2c.csm.application.service.metric.MetricsService;
import com.c2c.csm.infrastructure.registry.dto.RoomSummary;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class OnlineCommandHandler extends AbstractCommandHandler {
    private final RoomRegistryService roomRegistryService;

    public OnlineCommandHandler(
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
        return Action.ONLINE;
    }

    public record OnlinePayload(String roomId) {}

    @Override
    protected Object doHandle(Command command) {
        OnlinePayload payload = parsePayload(command.getPayload(), OnlinePayload.class);
        String userId = command.getUserId();
        String roomId = payload.roomId();
        log.info("command: online start userId={}, roomId={}", userId, roomId);

        PresenceResult presenceResult = roomRegistryService.markOnline(roomId, userId);

        Map<String, Object> notifyPayload = presenceResult.notifyPayload();
        presenceResult.onlineMembers().forEach(targetUserId -> {
            if (targetUserId.equals(userId)) {
                return;
            }
            Event event = buildEvent(command, targetUserId, EventType.NOTIFY, Action.ONLINE, notifyPayload, Status.SUCCESS);
            sendEvent(event);
        });

        RoomSummary summary = roomRegistryService.getRoomSummary(roomId);
        log.info("command: online success userId={}, roomId={}", userId, roomId);
        return summary;
    }
}
