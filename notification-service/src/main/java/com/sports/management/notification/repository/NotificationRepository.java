package com.sports.management.notification.repository;

import com.sports.management.notification.model.Notification;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long>, JpaSpecificationExecutor<Notification> {
    List<Notification> findByRecipientOrderByCreatedAtDesc(String recipient);
    List<Notification> findAllByOrderByCreatedAtDesc();
    long countByIsRead(boolean isRead);

    default Page<Notification> searchNotifications(
            String recipient,
            Boolean read,
            String keyword,
            Pageable pageable) {
        return findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (recipient != null && !recipient.isBlank()) {
                predicates.add(cb.equal(cb.lower(cb.coalesce(root.get("recipient"), "")), recipient.trim().toLowerCase(Locale.ROOT)));
            }
            if (read != null) {
                predicates.add(cb.equal(root.get("isRead"), read));
            }
            if (keyword != null && !keyword.isBlank()) {
                String pattern = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(cb.coalesce(root.get("title"), "")), pattern),
                        cb.like(cb.lower(cb.coalesce(root.get("message"), "")), pattern)
                ));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        }, pageable);
    }
}
