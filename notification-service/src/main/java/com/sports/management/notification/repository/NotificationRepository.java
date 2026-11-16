package com.sports.management.notification.repository;

import com.sports.management.notification.model.Notification;
import com.sports.management.notification.model.NotificationRead;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
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
    boolean existsByTypeAndReferenceId(String type, Long referenceId);

    default Page<Notification> searchNotifications(
            String recipient,
            Boolean read,
            String keyword,
            Pageable pageable) {
        return searchNotificationsForUser(recipient, read, keyword, null, pageable);
    }

    default Page<Notification> searchNotificationsForUser(
            String recipient,
            Boolean read,
            String keyword,
            String viewerUsername,
            Pageable pageable) {
        return findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (recipient != null && !recipient.isBlank()) {
                predicates.add(cb.equal(cb.lower(cb.coalesce(root.get("recipient"), "")), recipient.trim().toLowerCase(Locale.ROOT)));
            }
            if (read != null) {
                Predicate targetReadPredicate = cb.and(
                        cb.notEqual(cb.lower(cb.coalesce(root.get("recipient"), "")), "all_users"),
                        cb.isTrue(root.get("isRead"))
                );
                Predicate effectiveReadPredicate = targetReadPredicate;
                if (viewerUsername != null && !viewerUsername.isBlank()) {
                    Subquery<Long> sq = query.subquery(Long.class);
                    Root<NotificationRead> nr = sq.from(NotificationRead.class);
                    sq.select(nr.get("id")).where(
                            cb.equal(nr.get("notificationId"), root.get("id")),
                            cb.equal(cb.lower(nr.get("username")), viewerUsername.trim().toLowerCase(Locale.ROOT))
                    );
                    effectiveReadPredicate = cb.or(cb.exists(sq), targetReadPredicate);
                }
                predicates.add(Boolean.TRUE.equals(read) ? effectiveReadPredicate : cb.not(effectiveReadPredicate));
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
