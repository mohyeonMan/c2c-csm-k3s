package com.c2c.csm.infrastructure.registry;

import java.time.Instant;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class Room {
	private final String roomId;
	private final String ownerId;
	private final Instant createdAt;


}