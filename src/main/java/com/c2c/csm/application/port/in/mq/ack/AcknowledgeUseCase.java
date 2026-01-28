package com.c2c.csm.application.port.in.mq.ack;

import com.c2c.csm.application.model.Ack;

public interface AcknowledgeUseCase {
    
    void acknowledgeEvent(Ack ack);

}
