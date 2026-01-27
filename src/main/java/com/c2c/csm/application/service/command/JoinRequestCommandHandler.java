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
public class JoinRequestCommandHandler extends AbstractCommandHandler{
    private final RoomRegistry roomRegistry;
    
    public JoinRequestCommandHandler(
        EventPublishUsecase eventPublishUsecase,
        SessionPresencePort sessionPresencePort,
        CommonMapper commonMapper,
        RoomRegistry roomRegistry
    ) {
        super(eventPublishUsecase, sessionPresencePort, commonMapper);
        this.roomRegistry = roomRegistry;
    }

    public record JoinRequestPayload(String roomId, String nickName) {}

    @Override
    public Action supports() {
        return Action.JOIN_REQUEST;
    }

    @Override
    protected Object doHandle(Command command) {

        //검증 필요.
        JoinRequestPayload payload = parsePayload(command.getPayload(), JoinRequestPayload.class);
        String requestedUserId = command.getUserId();
        String targetRoomId = payload.roomId();
        String nickName = payload.nickName();

        // 토큰이 이미 있는 경우.
        boolean hasToken = roomRegistry.hasJoinApproveToken(targetRoomId, requestedUserId);
        if(hasToken) return approveDirectly(command, requestedUserId, targetRoomId);
        
        // 본인이 만든 방인 경우.
        String ownerId = roomRegistry.findOwnerId(targetRoomId).orElseThrow(() -> new RuntimeException(" 방을 찾을 수 없음."));
        if(ownerId.equals(requestedUserId)) {
            return approveDirectly(command, requestedUserId, targetRoomId);
        }

        Object joinRequestPayload = Map.of(
            "requestedUserId", requestedUserId,
            "nickname", nickName,
            "roomId", targetRoomId
        );

        // onwer에게 알림.
        Event requestEvent = buildEvent(command, ownerId, EventType.NOTIFY, Action.JOIN_REQUEST, joinRequestPayload, Status.SUCCESS);
        sendEvent(requestEvent);

        return joinRequestPayload;
    }

    private Object approveDirectly(
        Command command,
        String requestedUserId,
        String targetRoomId
    ){
        Object joinApprovePayload = Map.of(
            "requestedUserId", requestedUserId,
            "roomId", targetRoomId,
            "approved", true
        );
        Event approvedEvent = buildEvent(command, requestedUserId, EventType.NOTIFY, Action.JOIN_APPROVE, joinApprovePayload, Status.SUCCESS);
        sendEvent(approvedEvent);
        return joinApprovePayload;
    }
}
