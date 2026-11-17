package com.sports.management.notification.exception;

import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.Map;

public class ErrorResponse {

    private String timestamp;
    private int status;
    private String error;
    private String message;
    private String path;
    private Map<String, String> errors;

    public ErrorResponse() {
        this.timestamp = Instant.now().toString();
    }

    public ErrorResponse(int status, String message, Map<String, String> errors) {
        this(status, resolveErrorPhrase(status), message, null, errors);
    }

    public ErrorResponse(int status, String error, String message, String path, Map<String, String> errors) {
        this.timestamp = Instant.now().toString();
        this.status = status;
        this.error = (error != null && !error.isBlank()) ? error : resolveErrorPhrase(status);
        this.message = message;
        this.path = path;
        this.errors = errors;
    }

    private static String resolveErrorPhrase(int status) {
        HttpStatus resolved = HttpStatus.resolve(status);
        return resolved != null ? resolved.getReasonPhrase() : "HTTP " + status;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Map<String, String> getErrors() {
        return errors;
    }

    public void setErrors(Map<String, String> errors) {
        this.errors = errors;
    }
}
