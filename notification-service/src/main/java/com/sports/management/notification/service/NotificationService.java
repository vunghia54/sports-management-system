package com.sports.management.notification.service;

import com.sports.management.notification.model.Notification;
import com.sports.management.notification.repository.NotificationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public List<Notification> getAllNotifications() {
        return notificationRepository.findAllByOrderByCreatedAtDesc();
    }

    public Page<Notification> searchNotifications(String recipient, Boolean read, String keyword, Pageable pageable) {
        String safeRec = (recipient != null && !recipient.isBlank()) ? recipient.trim() : null;
        String safeKw = (keyword != null && !keyword.isBlank()) ? keyword.trim() : null;
        return notificationRepository.searchNotifications(safeRec, read, safeKw, pageable);
    }

    public Map<String, Object> getNotificationStatistics() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalNotifications", notificationRepository.count());
        stats.put("unreadNotifications", notificationRepository.countByIsRead(false));
        stats.put("readNotifications", notificationRepository.countByIsRead(true));
        return stats;
    }

    public List<Notification> getNotificationsByRecipient(String recipient) {
        return notificationRepository.findByRecipientOrderByCreatedAtDesc(recipient);
    }

    public Notification getNotificationById(Long id) {
        return notificationRepository.findById(id).orElse(null);
    }

    @Transactional
    public Notification createNotification(Notification notification) {
        notification.setId(null);
        if (notification.getCreatedAt() == null) {
            notification.setCreatedAt(LocalDateTime.now());
        }
        return notificationRepository.save(notification);
    }

    @Transactional
    public Notification markAsRead(Long id) {
        Notification notification = getNotificationById(id);
        if (notification != null) {
            notification.setRead(true);
            return notificationRepository.save(notification);
        }
        return null;
    }

    @Transactional
    public boolean deleteNotification(Long id) {
        if (id == null || !notificationRepository.existsById(id)) {
            return false;
        }
        notificationRepository.deleteById(id);
        return true;
    }
}
