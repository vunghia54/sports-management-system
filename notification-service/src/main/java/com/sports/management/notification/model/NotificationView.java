package com.sports.management.notification.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public record NotificationView(
        Long id,
        String recipient,
        String title,
        String message,
        String type,
        String eventType,
        Long referenceId,
        LocalDateTime createdAt,
        @JsonProperty("isRead") boolean isRead,
        boolean read
) {
    public static NotificationView from(Notification n, boolean readForCaller) {
        return new NotificationView(
                n.getId(),
                n.getRecipient(),
                n.getTitle(),
                n.getMessage(),
                n.getType(),
                n.getType(),
                n.getReferenceId(),
                n.getCreatedAt(),
                readForCaller,
                readForCaller
        );
    }
}
