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
import com.c2c.csm.infrastructure.registry.dto.RoomSummary;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class JoinCommandHandler extends AbstractCommandHandler{
    private final RoomRegistry roomRegistry;

    public JoinCommandHandler(
        EventPublishUsecase eventPublishUsecase,
        SessionPresencePort sessionPresencePort,
        CommonMapper commonMapper,
        RoomRegistry roomRegistry
    ) {
        super(eventPublishUsecase, sessionPresencePort, commonMapper);
        this.roomRegistry = roomRegistry;
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
        
        boolean hasToken = roomRegistry.hasJoinApproveToken(targetRoomId, joiningUserId);

        if(!hasToken) throw new RuntimeException("입장권한 없음.");

        boolean aleadyJoined = roomRegistry.isMember(targetRoomId, joiningUserId);
        if(aleadyJoined) throw new RuntimeException("이미 join한 상태.");

        boolean joined = roomRegistry.addMemberWithNickname(targetRoomId, joiningUserId, nickName);

        if(!joined) throw new RuntimeException("입장 실패.");

        RoomSummary summary = roomRegistry.getRoomSummary(targetRoomId).orElseThrow(() -> new RuntimeException());

        Object notifyPayload = Map.of(
            "userId", joiningUserId,
            "nickname", nickName
        );


        //참여자들에게 알림.
        summary.getEntries().stream().forEach(entry -> {
            String targetUserId = entry.getUserId();
            Event event = buildEvent(command, targetUserId, EventType.NOTIFY, Action.JOIN, notifyPayload, Status.SUCCESS);
            sendEvent(event);
        });
        
        return summary;
    }
    
}
