package com.sports.management.exception;

public class PermanentSyncException extends RuntimeException {

    private final int statusCode;

    public PermanentSyncException(int statusCode, String message, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
