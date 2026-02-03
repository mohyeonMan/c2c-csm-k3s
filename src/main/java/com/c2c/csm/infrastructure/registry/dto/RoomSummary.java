package com.c2c.csm.infrastructure.registry.dto;

import java.util.List;
import java.time.Instant;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RoomSummary {
    private final String roomId;
    private final String ownerId;
    private final List<RoomEntry> entries;
    private final Instant autoDeleteAt;
}
