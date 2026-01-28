package com.c2c.csm.application.service.command;

import org.springframework.stereotype.Service;

import com.c2c.csm.application.model.Action;
import com.c2c.csm.application.model.Command;
import com.c2c.csm.application.model.Event;
import com.c2c.csm.application.model.EventType;
import com.c2c.csm.application.model.Status;
import com.c2c.csm.application.port.out.event.EventPublishUsecase;
import com.c2c.csm.application.port.out.presenece.SessionPresencePort;
import com.c2c.csm.application.service.room.RoomRegistryService;
import com.c2c.csm.common.util.CommonMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class JoinApproveCommandHandler extends AbstractCommandHandler{
    private final RoomRegistryService roomRegistryService;
    
    public JoinApproveCommandHandler(
        EventPublishUsecase eventPublishUsecase,
        SessionPresencePort sessionPresencePort,
        CommonMapper commonMapper,
        RoomRegistryService roomRegistryService
    ) {
        super(eventPublishUsecase, sessionPresencePort, commonMapper);
        this.roomRegistryService = roomRegistryService;
    }

    public record JoinApprovePayload(String roomId, String requestedUserId, boolean approved) {}

    @Override
    public Action supports() {
        return Action.JOIN_APPROVE;
    }

    @Override
    protected Object doHandle(Command command) {
        
        JoinApprovePayload payload = parsePayload(command.getPayload(), JoinApprovePayload.class);
        String userId = command.getUserId();
        String targetRoomId = payload.roomId();
        String requestedUserId = payload.requestedUserId();
        boolean approved = payload.approved();
        log.info(
            "command: join approve start ownerId={}, roomId={}, requestedUserId={}, approved={}",
            userId,
            targetRoomId,
            requestedUserId,
            approved
        );
        
        Object joinApprovePayload = roomRegistryService.approveJoin(targetRoomId, userId, requestedUserId, approved);

        log.info(
            "command: join approve notify requestedUserId={}, roomId={}, approved={}",
            requestedUserId,
            targetRoomId,
            approved
        );
        Event approvedEvent = buildEvent(command, requestedUserId, EventType.NOTIFY, Action.JOIN_APPROVE, joinApprovePayload, Status.SUCCESS);
        sendEvent(approvedEvent);

        return joinApprovePayload;


    }
}
