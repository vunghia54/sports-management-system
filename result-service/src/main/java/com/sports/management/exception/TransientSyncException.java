package com.sports.management.exception;

public class TransientSyncException extends RuntimeException {

    public TransientSyncException(String message, Throwable cause) {
        super(message, cause);
    }
}
