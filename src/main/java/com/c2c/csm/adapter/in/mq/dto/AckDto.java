package com.c2c.csm.adapter.in.mq.dto;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class AckDto {
    private String ackId;
    private String eventId;
    private String sentAt;
}