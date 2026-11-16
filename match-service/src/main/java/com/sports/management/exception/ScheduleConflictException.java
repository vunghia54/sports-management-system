package com.sports.management.exception;

import java.util.List;
import java.util.Map;

public class ScheduleConflictException extends RuntimeException {

    private final List<Map<String, Object>> conflicts;

    public ScheduleConflictException(String message, List<Map<String, Object>> conflicts) {
        super(message);
        this.conflicts = conflicts != null ? conflicts : List.of();
    }

    public List<Map<String, Object>> getConflicts() {
        return conflicts;
    }
}
