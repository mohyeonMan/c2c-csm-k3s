package com.c2c.csm.application.model;

import java.time.Instant;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString
public class Event {
    private final String requestId;
    private final String commandId;
    private final String userId;
    private final String eventId;
    private final EventType type;
    private final Action action;
    private final String payload;
    private final Status status;
    private final Instant sentAt;
}