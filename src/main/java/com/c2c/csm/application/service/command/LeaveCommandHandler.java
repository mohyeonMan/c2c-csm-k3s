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
import com.c2c.csm.application.service.room.RoomRegistryService.LeaveResult;
import com.c2c.csm.common.util.CommonMapper;

import com.c2c.csm.application.service.metric.MetricsService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class LeaveCommandHandler extends AbstractCommandHandler{
    private final RoomRegistryService roomRegistryService;

    public LeaveCommandHandler(
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
        return Action.LEAVE;
    }

    public record LeavePayload(String roomId) {}

    @Override
    protected Object doHandle(Command command) {
        LeavePayload payload = parsePayload(command.getPayload(), LeavePayload.class);
        String leavingUserId = command.getUserId();
        String targetRoomId = payload.roomId();
        log.info("command: leave start userId={}, roomId={}", leavingUserId, targetRoomId);

        LeaveResult leaveResult = roomRegistryService.leaveRoom(targetRoomId, leavingUserId);
        Map<String, Object> notifyPayload = leaveResult.notifyPayload();

        leaveResult.remainingMembers().forEach(targetUserId -> {
            Event event = buildEvent(command, targetUserId, EventType.NOTIFY, Action.LEAVE, notifyPayload, Status.SUCCESS);
            sendEvent(event);
        });

        log.info("command: leave success userId={}, roomId={}", leavingUserId, targetRoomId);
        return Map.of(
            "roomId", leaveResult.roomId()
            );
    }
}
