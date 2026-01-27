package com.c2c.csm.adapter.in.mq.dto;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class CommandDto {
    private String commandId;
    private String requestId;
    private String userId; 
    private String action;
    private String payload;
    private String sentAt;
}