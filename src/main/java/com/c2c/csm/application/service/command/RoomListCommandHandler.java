package com.c2c.csm.application.service.command;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.c2c.csm.application.model.Action;
import com.c2c.csm.application.model.Command;
import com.c2c.csm.application.port.out.event.EventPublishUsecase;
import com.c2c.csm.application.port.out.presence.SessionPresencePort;
import com.c2c.csm.application.service.room.RoomRegistryService;
import com.c2c.csm.common.util.CommonMapper;

import com.c2c.csm.application.service.metric.MetricsService;
import com.c2c.csm.infrastructure.registry.dto.RoomSummary;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class RoomListCommandHandler extends AbstractCommandHandler {
    private final RoomRegistryService roomRegistryService;

    public RoomListCommandHandler(
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
        return Action.ROOM_LIST;
    }

    @Override
    protected Object doHandle(Command command) {
        String userId = command.getUserId();
        log.info("command: room list start userId={}", userId);

        List<RoomSummary> summaries = roomRegistryService.listRoomSummaries(userId);
        Map<String, Object> result = Map.of(
            "rooms", summaries,
            "count", summaries.size()
        );

        log.info("command: room list success userId={}, rooms={}", userId, summaries.size());
        return result;
    }
}
