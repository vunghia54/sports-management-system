package com.sports.management.notification.service;

import com.sports.management.notification.model.Notification;
import com.sports.management.notification.model.NotificationRead;
import com.sports.management.notification.model.NotificationView;
import com.sports.management.notification.repository.NotificationReadRepository;
import com.sports.management.notification.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class NotificationService {

    private static final String ALL_USERS = "ALL_USERS";

    private final NotificationRepository notificationRepository;
    private final NotificationReadRepository notificationReadRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this(notificationRepository, null);
    }

    @Autowired
    public NotificationService(
            NotificationRepository notificationRepository,
            NotificationReadRepository notificationReadRepository) {
        this.notificationRepository = notificationRepository;
        this.notificationReadRepository = notificationReadRepository;
    }

    public List<Notification> getAllNotifications() {
        return notificationRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<NotificationView> getAllNotificationViews(String viewerUsername) {
        return getAllNotificationViews(viewerUsername, false);
    }

    public List<NotificationView> getAllNotificationViews(String viewerUsername, boolean isAdmin) {
        Set<Long> readIds = resolveReadIds(viewerUsername);
        return notificationRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(n -> canViewNotification(n, viewerUsername, isAdmin))
                .map(n -> NotificationView.from(n, isReadByUser(n, viewerUsername, readIds)))
                .toList();
    }

    public Page<NotificationView> searchNotificationsForUser(
            String recipient, Boolean read, String keyword, String viewerUsername, Pageable pageable) {
        return searchNotificationsForUser(recipient, read, keyword, viewerUsername, false, pageable);
    }

    public Page<NotificationView> searchNotificationsForUser(
            String recipient, Boolean read, String keyword, String viewerUsername, boolean isAdmin, Pageable pageable) {
        String safeRec = (recipient != null && !recipient.isBlank()) ? recipient.trim() : null;
        if (safeRec != null) {
            assertCanAccessRecipient(safeRec, viewerUsername, isAdmin);
        }
        String safeKw = (keyword != null && !keyword.isBlank()) ? keyword.trim() : null;
        Set<Long> readIds = resolveReadIds(viewerUsername);
        return notificationRepository.searchNotificationsForUser(safeRec, read, safeKw, viewerUsername, isAdmin, pageable)
                .map(n -> NotificationView.from(n, isReadByUser(n, viewerUsername, readIds)));
    }

    public Page<Notification> searchNotifications(String recipient, Boolean read, String keyword, Pageable pageable) {
        String safeRec = (recipient != null && !recipient.isBlank()) ? recipient.trim() : null;
        String safeKw = (keyword != null && !keyword.isBlank()) ? keyword.trim() : null;
        return notificationRepository.searchNotifications(safeRec, read, safeKw, pageable);
    }

    public Map<String, Object> getNotificationStatistics() {
        return getNotificationStatistics(null, false);
    }

    public Map<String, Object> getNotificationStatistics(String viewerUsername) {
        return getNotificationStatistics(viewerUsername, false);
    }

    public Map<String, Object> getNotificationStatistics(String viewerUsername, boolean isAdmin) {
        List<Notification> visible = notificationRepository.findAll().stream()
                .filter(n -> canViewNotification(n, viewerUsername, isAdmin))
                .toList();
        Set<Long> readIds = resolveReadIds(viewerUsername);
        long readCount = visible.stream().filter(n -> isReadByUser(n, viewerUsername, readIds)).count();
        long total = visible.size();
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalNotifications", total);
        stats.put("unreadNotifications", total - readCount);
        stats.put("readNotifications", readCount);
        return stats;
    }

    public List<Notification> getNotificationsByRecipient(String recipient) {
        return notificationRepository.findByRecipientOrderByCreatedAtDesc(recipient);
    }

    public List<NotificationView> getNotificationViewsByRecipient(String recipient, String viewerUsername) {
        return getNotificationViewsByRecipient(recipient, viewerUsername, false);
    }

    public List<NotificationView> getNotificationViewsByRecipient(String recipient, String viewerUsername, boolean isAdmin) {
        assertCanAccessRecipient(recipient, viewerUsername, isAdmin);
        Set<Long> readIds = resolveReadIds(viewerUsername);
        return notificationRepository.findByRecipientOrderByCreatedAtDesc(recipient).stream()
                .filter(n -> canViewNotification(n, viewerUsername, isAdmin))
                .map(n -> NotificationView.from(n, isReadByUser(n, viewerUsername, readIds)))
                .toList();
    }

    public Notification getNotificationById(Long id) {
        return notificationRepository.findById(id).orElse(null);
    }

    public NotificationView getNotificationViewById(Long id, String viewerUsername) {
        return getNotificationViewById(id, viewerUsername, false);
    }

    public NotificationView getNotificationViewById(Long id, String viewerUsername, boolean isAdmin) {
        Notification notification = getNotificationById(id);
        if (notification == null) {
            return null;
        }
        assertCanAccessRecipient(notification.getRecipient(), viewerUsername, isAdmin);
        Set<Long> readIds = resolveReadIds(viewerUsername);
        return NotificationView.from(notification, isReadByUser(notification, viewerUsername, readIds));
    }

    public boolean canViewNotification(Notification n, String viewerUsername, boolean isAdmin) {
        if (n == null) {
            return false;
        }
        if (isAdmin) {
            return true;
        }
        String target = n.getRecipient() != null ? n.getRecipient().trim() : "";
        if (ALL_USERS.equalsIgnoreCase(target)) {
            return true;
        }
        return viewerUsername != null && !viewerUsername.isBlank()
                && target.equalsIgnoreCase(viewerUsername.trim());
    }

    private void assertCanAccessRecipient(String recipient, String viewerUsername, boolean isAdmin) {
        if (isAdmin) {
            return;
        }
        String target = recipient != null ? recipient.trim() : "";
        if (ALL_USERS.equalsIgnoreCase(target)) {
            return;
        }
        if (viewerUsername == null || viewerUsername.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Yêu cầu đăng nhập để xem thông báo riêng");
        }
        if (!target.equalsIgnoreCase(viewerUsername.trim())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Access Denied: Chỉ người nhận hoặc ADMIN mới được xem thông báo gửi riêng");
        }
    }

    @Transactional
    public Notification createNotification(Notification notification) {
        notification.setId(null);
        notification.setRead(false);
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
    public NotificationView markAsReadForUser(Long id, String username, boolean isAdmin) {
        if (username == null || username.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Full authentication is required to mark notifications as read");
        }
        Notification notification = getNotificationById(id);
        if (notification == null) {
            return null;
        }
        String trimmedUser = username.trim();
        boolean isBroadcast = ALL_USERS.equalsIgnoreCase(notification.getRecipient());
        if (!isBroadcast && !isAdmin) {
            if (notification.getRecipient() == null || !notification.getRecipient().equalsIgnoreCase(trimmedUser)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Access Denied: Bạn không thể đánh dấu đã đọc thông báo của người khác");
            }
        }

        if (notificationReadRepository != null
                && !notificationReadRepository.existsByNotificationIdAndUsernameIgnoreCase(id, trimmedUser)) {
            notificationReadRepository.save(new NotificationRead(id, trimmedUser));
        }

        if (!isBroadcast) {
            notification.setRead(true);
            notification = notificationRepository.save(notification);
        }
        return NotificationView.from(notification, true);
    }

    @Transactional
    public boolean deleteNotification(Long id) {
        if (id == null || !notificationRepository.existsById(id)) {
            return false;
        }
        if (notificationReadRepository != null) {
            notificationReadRepository.deleteByNotificationId(id);
        }
        notificationRepository.deleteById(id);
        return true;
    }

    private Set<Long> resolveReadIds(String viewerUsername) {
        if (notificationReadRepository == null || viewerUsername == null || viewerUsername.isBlank()) {
            return Collections.emptySet();
        }
        return notificationReadRepository.findReadNotificationIdsByUsername(viewerUsername.trim());
    }

    private boolean isReadByUser(Notification n, String viewerUsername, Set<Long> readIds) {
        boolean isBroadcast = ALL_USERS.equalsIgnoreCase(n.getRecipient());
        if (isBroadcast) {
            return viewerUsername != null && !viewerUsername.isBlank() && readIds.contains(n.getId());
        }
        return Boolean.TRUE.equals(n.getRead())
                || (viewerUsername != null && !viewerUsername.isBlank() && readIds.contains(n.getId()));
    }
}
