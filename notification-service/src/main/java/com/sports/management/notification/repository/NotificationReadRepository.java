package com.sports.management.notification.repository;

import com.sports.management.notification.model.NotificationRead;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Set;

@Repository
public interface NotificationReadRepository extends JpaRepository<NotificationRead, Long> {

    boolean existsByNotificationIdAndUsernameIgnoreCase(Long notificationId, String username);

    @Query("SELECT nr.notificationId FROM NotificationRead nr WHERE lower(nr.username) = lower(:username)")
    Set<Long> findReadNotificationIdsByUsername(@Param("username") String username);

    void deleteByNotificationId(Long notificationId);
}
