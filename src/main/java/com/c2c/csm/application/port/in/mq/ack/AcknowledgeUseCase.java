package com.c2c.csm.application.port.in.mq.ack;

import com.c2c.csm.adapter.in.mq.dto.AckDto;

public interface AcknowledgeUseCase {
    
    void acknowledgeEvent(AckDto ackDto);

}
