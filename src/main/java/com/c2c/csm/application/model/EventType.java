package com.c2c.csm.application.model;

import java.util.Locale;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;

public enum EventType {
    ACK,
    RESULT,
    SYSTEM,
    NOTIFY,
    MESSAGE,
    EVENT,
    ERROR,
    @JsonEnumDefaultValue
    UNKNOWN;

    @JsonCreator
    public static EventType from(String value) {
        if (value == null) {
            return UNKNOWN;
        }
        try {
            return EventType.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return UNKNOWN;
        }
    }
}