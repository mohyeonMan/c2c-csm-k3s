package com.c2c.csm.application.service.command;

import org.springframework.stereotype.Service;

import com.c2c.csm.application.model.Action;
import com.c2c.csm.application.model.Command;
import com.c2c.csm.application.port.out.event.EventPublishUsecase;
import com.c2c.csm.application.port.out.presenece.SessionPresencePort;
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
        throw new RuntimeException("Unknown action: " + command.getAction());
    }
}
