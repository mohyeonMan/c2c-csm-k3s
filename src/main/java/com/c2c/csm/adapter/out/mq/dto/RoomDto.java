package com.c2c.csm.adapter.out.mq.dto;


import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class RoomDto {
    private final String roomId;
	private final String ownerId;
	private final String createdAt;
}
