package com.c2c.csm.application.port.in.mq.command;

import com.c2c.csm.adapter.in.mq.dto.CommandDto;

public interface ConsumeCommandPort {

    void onCommand(CommandDto command);
}
