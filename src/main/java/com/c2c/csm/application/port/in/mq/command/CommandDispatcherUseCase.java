package com.c2c.csm.application.port.in.mq.command;

import com.c2c.csm.application.model.Command;

public interface CommandDispatcherUseCase {
    
    void dispatchCommand(Command command);

}
