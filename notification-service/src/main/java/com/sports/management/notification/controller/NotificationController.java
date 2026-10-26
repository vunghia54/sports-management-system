package com.sports.management.notification.controller;

import com.sports.management.notification.model.Notification;
import com.sports.management.notification.service.NotificationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ResponseEntity<List<Notification>> getAllNotifications(
            @RequestParam(required = false) String recipient) {
        if (recipient != null && !recipient.isBlank()) {
            return ResponseEntity.ok(notificationService.getNotificationsByRecipient(recipient));
        }
        return ResponseEntity.ok(notificationService.getAllNotifications());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Notification> getNotificationById(@PathVariable Long id) {
        Notification notification = notificationService.getNotificationById(id);
        if (notification == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found with id: " + id);
        }
        return ResponseEntity.ok(notification);
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<Notification> markAsRead(@PathVariable Long id) {
        Notification notification = notificationService.markAsRead(id);
        if (notification == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found with id: " + id);
        }
        return ResponseEntity.ok(notification);
    }
}
