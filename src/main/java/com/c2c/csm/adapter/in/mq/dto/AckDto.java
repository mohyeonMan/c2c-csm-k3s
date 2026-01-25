package com.c2c.csm.adapter.in.mq.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString
public class AckDto {
    private final String ackId;
    private final String eventId;
    private final String sentAt;

}