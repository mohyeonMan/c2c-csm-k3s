package com.c2c.csm.application.port.out.event;

import com.c2c.csm.application.model.Event;

public interface PublishEventPort {

    void publishEvent(String routingKey, Event event);

}
