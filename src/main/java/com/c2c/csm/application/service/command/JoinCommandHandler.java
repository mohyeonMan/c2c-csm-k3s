package com.c2c.csm.application.service.command;

import org.springframework.stereotype.Service;

import java.util.Map;

import com.c2c.csm.application.model.Action;
import com.c2c.csm.application.model.Command;
import com.c2c.csm.application.model.Event;
import com.c2c.csm.application.model.EventType;
import com.c2c.csm.application.model.Status;
import com.c2c.csm.application.port.out.event.EventPublishUsecase;
import com.c2c.csm.application.port.out.presence.SessionPresencePort;
import com.c2c.csm.common.util.CommonMapper;

import com.c2c.csm.application.service.metric.MetricsService;
import com.c2c.csm.application.service.room.RoomRegistryService;
import com.c2c.csm.application.service.room.RoomRegistryService.JoinResult;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class JoinCommandHandler extends AbstractCommandHandler{
    private final RoomRegistryService roomRegistryService;

    public JoinCommandHandler(
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
        return Action.JOIN;
    }

    public record JoinPayload(String roomId, String nickName) {}

    @Override
    protected Object doHandle(Command command) {

        //ê²€ì¦??„ìš”.
        JoinPayload payload = parsePayload(command.getPayload(), JoinPayload.class);
        String joiningUserId = command.getUserId();
        String targetRoomId = payload.roomId();
        String nickName = payload.nickName();
        log.info(
            "command: join start userId={}, roomId={}, nickname={}",
            joiningUserId,
            targetRoomId,
            nickName
        );
        
        JoinResult joinResult = roomRegistryService.joinRoom(targetRoomId, joiningUserId, nickName);
        Object notifyPayload = joinResult.notifyPayload();


        //ì°¸ì—¬?ë“¤?ê²Œ ?Œë¦¼.
        joinResult.onlineMembers().forEach(targetUserId -> {
            Event event = buildEvent(command, targetUserId, EventType.NOTIFY, Action.JOIN, notifyPayload, Status.SUCCESS);
            sendEvent(event);
        });
        log.info(
            "command: join success userId={}, roomId={}, onlineMembers={}",
            joiningUserId,
            targetRoomId,
            joinResult.onlineMembers().size()
        );
        return Map.of(
            "roomId", targetRoomId,
            "userId", joiningUserId,
            "nickname", nickName
        );
    }
    
}
