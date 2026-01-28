package com.c2c.csm.application.service.command;

import org.springframework.stereotype.Service;

import com.c2c.csm.application.model.Action;
import com.c2c.csm.application.model.Command;
import com.c2c.csm.application.model.Event;
import com.c2c.csm.application.model.EventType;
import com.c2c.csm.application.model.Status;
import com.c2c.csm.application.port.out.event.EventPublishUsecase;
import com.c2c.csm.application.port.out.presenece.SessionPresencePort;
import com.c2c.csm.common.util.CommonMapper;
import com.c2c.csm.application.service.room.RoomRegistryService;
import com.c2c.csm.application.service.room.RoomRegistryService.JoinResult;
import com.c2c.csm.infrastructure.registry.dto.RoomSummary;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class JoinCommandHandler extends AbstractCommandHandler{
    private final RoomRegistryService roomRegistryService;

    public JoinCommandHandler(
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
        return Action.JOIN;
    }

    public record JoinPayload(String roomId, String nickName) {}

    @Override
    protected Object doHandle(Command command) {

        //검증 필요.
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
        RoomSummary summary = joinResult.summary();


        //참여자들에게 알림.
        summary.getEntries().stream().forEach(entry -> {
            String targetUserId = entry.getUserId();
            Event event = buildEvent(command, targetUserId, EventType.NOTIFY, Action.JOIN, notifyPayload, Status.SUCCESS);
            sendEvent(event);
        });
        log.info(
            "command: join success userId={}, roomId={}, members={}",
            joiningUserId,
            targetRoomId,
            summary.getEntries().size()
        );
        return summary;
    }
    
}
