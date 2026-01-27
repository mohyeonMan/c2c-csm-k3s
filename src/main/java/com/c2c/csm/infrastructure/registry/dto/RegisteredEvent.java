package com.c2c.csm.infrastructure.registry.dto;

import java.time.Instant;

import com.c2c.csm.application.model.Event;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RegisteredEvent {
    private final Event event; 
    private final long retryCount;
    private final Instant nextAttemptAt;
}
