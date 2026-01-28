package com.c2c.csm.application.port.in.mq.command;

import com.c2c.csm.application.model.Action;
import com.c2c.csm.application.model.Command;


public interface CommandHandler {
    
    Action supports();

    void handle(Command command);

}
