package com.c2c.csm.application.model;

import java.util.Locale;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;

public enum Action {
    CONN_CLOSED,
    ROOM_CREATE,
    JOIN,
    JOIN_REQUEST,
    JOIN_APPROVE,
    LEAVE,
    CLIENT_MESSAGE,
    @JsonEnumDefaultValue
    UNKNOWN;

    @JsonCreator
    public static Action from(String value) {
        if (value == null) {
            return UNKNOWN;
        }
        try {
            return Action.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return UNKNOWN;
        }
    }
}
