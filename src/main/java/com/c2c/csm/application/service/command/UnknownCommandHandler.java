package com.c2c.csm.application.service.command;

import org.springframework.stereotype.Service;

import com.c2c.csm.application.model.Action;
import com.c2c.csm.application.model.Command;
import com.c2c.csm.application.port.out.event.EventPublishUsecase;
import com.c2c.csm.application.port.out.presence.SessionPresencePort;
import com.c2c.csm.common.exception.C2cException;
import com.c2c.csm.common.exception.ErrorCode;
import com.c2c.csm.common.util.CommonMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class UnknownCommandHandler extends AbstractCommandHandler {
    public UnknownCommandHandler(
        EventPublishUsecase eventPublishUsecase,
        SessionPresencePort sessionPresencePort,
        CommonMapper commonMapper
    ) {
        super(eventPublishUsecase, sessionPresencePort, commonMapper);
    }

    @Override
    public Action supports() {
        return Action.UNKNOWN;
    }

    @Override
    protected Object doHandle(Command command) {
        log.info(
            "command: unknown action={}, commandId={}, requestId={}, userId={}",
            command.getAction(),
            command.getCommandId(),
            command.getRequestId(),
            command.getUserId()
        );
        throw new C2cException(
            ErrorCode.CSM_UNSUPPORTED_ACTION,
            "지원하지 않는 액션입니다: " + command.getAction()
        );
    }
}
