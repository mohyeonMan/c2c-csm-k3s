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
import com.c2c.csm.common.util.CommonMapper;
import com.c2c.csm.infrastructure.registry.RoomRegistry;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class JoinApproveCommandHandler extends AbstractCommandHandler{
    private final RoomRegistry roomRegistry;
    
    public JoinApproveCommandHandler(
        EventPublishUsecase eventPublishUsecase,
        SessionPresencePort sessionPresencePort,
        CommonMapper commonMapper,
        RoomRegistry roomRegistry
    ) {
        super(eventPublishUsecase, sessionPresencePort, commonMapper);
        this.roomRegistry = roomRegistry;
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
        
        String ownerId = roomRegistry.findOwnerId(targetRoomId).orElseThrow(() -> new RuntimeException("방을 찾을 수 없음."));
        if(!ownerId.equals(userId)) throw new RuntimeException("방장 아님.");

        if(approved) roomRegistry.saveJoinApproveToken(targetRoomId, requestedUserId);
        else roomRegistry.revokeJoinApproveToken(targetRoomId, requestedUserId);
        
        Object joinApprovePayload = Map.of(
            "requestedUserId", requestedUserId,
            "roomId", targetRoomId,
            "approved", approved
        );

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
