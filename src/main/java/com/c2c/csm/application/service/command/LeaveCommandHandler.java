package com.c2c.csm.application.service.command;

import java.util.HashMap;
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
public class LeaveCommandHandler extends AbstractCommandHandler{
    private final RoomRegistry roomRegistry;

    public LeaveCommandHandler(
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
        return Action.LEAVE;
    }

    public record LeavePayload(String roomId) {}

    @Override
    protected Object doHandle(Command command) {
        LeavePayload payload = parsePayload(command.getPayload(), LeavePayload.class);
        String leavingUserId = command.getUserId();
        String targetRoomId = payload.roomId();
        log.info("command: leave start userId={}, roomId={}", leavingUserId, targetRoomId);

        String previousOwnerId = roomRegistry.findOwnerId(targetRoomId).orElseThrow(()-> new RuntimeException("방 없음."));

        boolean joined = roomRegistry.isMember(targetRoomId, leavingUserId);
        if(!joined) throw new RuntimeException("참여자 아님.");

        String nickname = roomRegistry.findMemberNickname(targetRoomId, leavingUserId)
            .orElseThrow(() -> new RuntimeException("닉네임을 찾을 수 없음."));

        boolean removed = roomRegistry.removeMember(targetRoomId, leavingUserId);
        if(!removed) throw new RuntimeException("나가기 실패");

        Map<String,Object> notifyPayload = new HashMap<String, Object>();
        notifyPayload.put("userId", leavingUserId);
        notifyPayload.put("nickname", nickname);

        if(previousOwnerId != null && previousOwnerId.equals(leavingUserId)) {
            roomRegistry.findOwnerId(targetRoomId).ifPresent(newOwnerId ->
                notifyPayload.put("newOwnerId", newOwnerId)
            );
        }

        roomRegistry.findMembers(targetRoomId).forEach(targetUserId -> {
            Event event = buildEvent(command, targetUserId, EventType.NOTIFY, Action.LEAVE, notifyPayload, Status.SUCCESS);
            sendEvent(event);
        });

        log.info("command: leave success userId={}, roomId={}", leavingUserId, targetRoomId);
        return Map.of(
            "roomId", targetRoomId
            );
    }
}
