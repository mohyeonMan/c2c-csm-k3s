package com.c2c.csm.common.exception;

public class C2cException extends RuntimeException {
    private final ErrorCode errorCode;

    public C2cException(ErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
    }

    public C2cException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public C2cException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
