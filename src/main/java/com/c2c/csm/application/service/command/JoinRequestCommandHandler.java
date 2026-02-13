package com.c2c.csm.application.service.command;

import org.springframework.stereotype.Service;

import com.c2c.csm.application.model.Action;
import com.c2c.csm.application.model.Command;
import com.c2c.csm.application.model.Event;
import com.c2c.csm.application.model.EventType;
import com.c2c.csm.application.model.Status;
import com.c2c.csm.application.port.out.event.EventPublishUsecase;
import com.c2c.csm.application.port.out.presence.SessionPresencePort;
import com.c2c.csm.application.service.room.RoomRegistryService;
import com.c2c.csm.application.service.room.RoomRegistryService.JoinRequestResult;
import com.c2c.csm.common.util.CommonMapper;

import com.c2c.csm.application.service.metric.MetricsService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class JoinRequestCommandHandler extends AbstractCommandHandler{
    private final RoomRegistryService roomRegistryService;
    
    public JoinRequestCommandHandler(
        EventPublishUsecase eventPublishUsecase,
        SessionPresencePort sessionPresencePort,
        CommonMapper commonMapper,
        MetricsService metricsService,
        RoomRegistryService roomRegistryService
    ) {
        super(eventPublishUsecase, sessionPresencePort, commonMapper, metricsService);
        this.roomRegistryService = roomRegistryService;
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
        log.info(
            "command: join request start userId={}, roomId={}, nickname={}",
            requestedUserId,
            targetRoomId,
            nickName
        );

        JoinRequestResult result = roomRegistryService.prepareJoinRequest(targetRoomId, requestedUserId, nickName);
        Action action = result.directApprove() ? Action.JOIN_APPROVE : Action.JOIN_REQUEST;

        if (result.directApprove()) {
            log.info(
                "command: join request direct approve userId={}, roomId={}",
                requestedUserId,
                targetRoomId
            );
        } else {
            log.info(
                "command: join request notify ownerId={}, requestedUserId={}, roomId={}",
                result.targetUserId(),
                requestedUserId,
                targetRoomId
            );
        }

        Event requestEvent = buildEvent(command, result.targetUserId(), EventType.NOTIFY, action, result.payload(), Status.SUCCESS);
        sendEvent(requestEvent);

        return result.payload();
    }
}
