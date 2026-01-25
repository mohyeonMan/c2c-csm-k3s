package com.c2c.csm.application.model;

import java.time.Instant;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString
public class Ack {

    private final String ackId;
    private final String eventId;
    private final Instant sentAt;
    
}

