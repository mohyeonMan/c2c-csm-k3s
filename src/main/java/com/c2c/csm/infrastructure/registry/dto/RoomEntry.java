package com.c2c.csm.infrastructure.registry.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RoomEntry {
    private final String userId;
    private final String nickname;
}
