package com.c2c.csm.adapter.out.mq.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventDto {
    private String requestId;
    private String frameId;
    private String userId;
    private String eventId;
    private String type;
    private String action;
    private String payload;
    private String status;
    private String sentAt;
}
