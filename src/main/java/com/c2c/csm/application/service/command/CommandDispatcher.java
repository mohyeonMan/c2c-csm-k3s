package com.c2c.csm.application.service.command;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.c2c.csm.application.model.Action;
import com.c2c.csm.application.model.Command;
import com.c2c.csm.application.port.in.mq.command.CommandDispatcherUseCase;
import com.c2c.csm.application.port.in.mq.command.CommandHandler;

import lombok.extern.slf4j.Slf4j;


@Slf4j
@Service
public class CommandDispatcher implements CommandDispatcherUseCase{
    private final Map<Action, CommandHandler> commandHandlerMap;

    public CommandDispatcher(List<CommandHandler> handlers) {
        Map<Action, CommandHandler> mapped = new EnumMap<>(Action.class);
        for (CommandHandler handler : handlers) {
            if (handler == null || handler.supports() == null) {
                continue;
            }
            mapped.put(handler.supports(), handler);
        }
        this.commandHandlerMap = mapped;
    }

    @Override
    public void dispatchCommand(Command command) {
        log.info(
            "command: dispatch start action={}, commandId={}, requestId={}, userId={}",
            command.getAction(),
            command.getCommandId(),
            command.getRequestId(),
            command.getUserId()
        );
        CommandHandler handler = commandHandlerMap.get(command.getAction());
        if (handler == null) {
            handler = commandHandlerMap.get(Action.UNKNOWN);
        }
        if (handler == null) {
            log.warn("No handler for action: {}", command.getAction());
            return;
        }
        log.info(
            "command: dispatch handler action={}, handler={}",
            command.getAction(),
            handler.getClass().getSimpleName()
        );
        handler.handle(command);
    }

}
