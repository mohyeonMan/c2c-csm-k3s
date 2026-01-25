package com.c2c.csm.application.port.out.mq;

import com.c2c.csm.application.model.Event;

public interface EventPublishUsecase {
    
    public void saveAndPublish(String routingKey, Event event);
    
}
