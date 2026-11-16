package com.sports.management.notification.controller;

import com.sports.management.notification.model.Notification;
import com.sports.management.notification.model.NotificationView;
import com.sports.management.notification.service.NotificationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id", "recipient", "title", "type", "referenceId", "createdAt", "isRead"
    );

    private final NotificationService notificationService;
    private final String instanceId;

    public NotificationController(
            NotificationService notificationService,
            @Value("${eureka.instance.instance-id:${spring.application.name}:${server.port}}") String instanceId) {
        this.notificationService = notificationService;
        this.instanceId = instanceId;
    }

    @GetMapping("/hello")
    public ResponseEntity<Map<String, String>> sayHello() {
        return ResponseEntity.ok(Map.of(
                "service", "Notification Service",
                "message", "Welcome to Notification Service API (TH1 - TH7)",
                "instanceId", instanceId
        ));
    }

    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getServiceInfo() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("serviceName", "NOTIFICATION-SERVICE");
        info.put("version", "v1");
        info.put("description", "Quản lý thông báo sự kiện giải đấu/trận đấu qua RabbitMQ (Event-Driven) và REST API");
        info.put("instanceId", instanceId);
        return ResponseEntity.ok(info);
    }

    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getNotificationStatistics(Authentication authentication) {
        String viewer = extractUsername(authentication);
        return ResponseEntity.ok(notificationService.getNotificationStatistics(viewer));
    }

    @GetMapping
    public ResponseEntity<List<NotificationView>> getAllNotifications(
            @RequestParam(required = false) String recipient,
            Authentication authentication) {
        String viewer = extractUsername(authentication);
        List<NotificationView> list = (recipient != null && !recipient.isBlank())
                ? notificationService.getNotificationViewsByRecipient(recipient, viewer)
                : notificationService.getAllNotificationViews(viewer);
        return ResponseEntity.ok()
                .header("X-Instance-Id", instanceId)
                .body(list);
    }

    @GetMapping("/paged")
    public ResponseEntity<Page<NotificationView>> searchNotifications(
            @RequestParam(required = false) String recipient,
            @RequestParam(required = false) Boolean read,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            Authentication authentication) {

        String resolvedSortBy = (sortBy == null || sortBy.isBlank()) ? "createdAt" : sortBy.trim();
        if (!ALLOWED_SORT_FIELDS.contains(resolvedSortBy)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "sortBy không hợp lệ: '" + sortBy + "'. Cho phép: " + ALLOWED_SORT_FIELDS);
        }
        Sort sort = "asc".equalsIgnoreCase(sortDir)
                ? Sort.by(resolvedSortBy).ascending()
                : Sort.by(resolvedSortBy).descending();
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(100, Math.max(1, size)), sort);
        String viewer = extractUsername(authentication);
        return ResponseEntity.ok(notificationService.searchNotificationsForUser(recipient, read, keyword, viewer, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<NotificationView> getNotificationById(
            @PathVariable Long id,
            Authentication authentication) {
        String viewer = extractUsername(authentication);
        NotificationView view = notificationService.getNotificationViewById(id, viewer);
        if (view == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found with id: " + id);
        }
        return ResponseEntity.ok()
                .header("X-Instance-Id", instanceId)
                .body(view);
    }

    @PostMapping
    public ResponseEntity<Notification> createNotification(@Valid @RequestBody Notification notification) {
        Notification created = notificationService.createNotification(notification);
        return ResponseEntity.status(HttpStatus.CREATED)
                .header("X-Instance-Id", instanceId)
                .body(created);
    }

    @RequestMapping(value = "/{id}/read", method = {RequestMethod.PATCH, RequestMethod.PUT})
    public ResponseEntity<NotificationView> markAsRead(
            @PathVariable Long id,
            Authentication authentication) {
        String username = extractUsername(authentication);
        boolean isAdmin = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equalsIgnoreCase(a.getAuthority()));
        NotificationView updated = notificationService.markAsReadForUser(id, username, isAdmin);
        if (updated == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found with id: " + id);
        }
        return ResponseEntity.ok()
                .header("X-Instance-Id", instanceId)
                .body(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotification(@PathVariable Long id) {
        if (!notificationService.deleteNotification(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found with id: " + id);
        }
        return ResponseEntity.noContent().build();
    }

    private String extractUsername(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equalsIgnoreCase(authentication.getName())) {
            return null;
        }
        return authentication.getName();
    }
}
