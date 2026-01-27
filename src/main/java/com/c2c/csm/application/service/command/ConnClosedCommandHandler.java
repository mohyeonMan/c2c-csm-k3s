package com.c2c.csm.application.service.command;

import com.c2c.csm.application.model.Action;
import com.c2c.csm.application.model.Command;
import com.c2c.csm.application.port.in.mq.command.CommandHandler;

public class ConnClosedCommandHandler implements CommandHandler{
  
    @Override
    public Action supports() {
        return Action.CONN_CLOSED;
    }
    
    @Override
    public void handle(Command message) {
        // TODO Auto-generated method stub
        
    }

}
