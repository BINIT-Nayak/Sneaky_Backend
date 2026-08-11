package com.sneaky.sneaky.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sneaky.sneaky.entity.Notification;
import com.sneaky.sneaky.entity.Users;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    List<Notification> findByUserOrderByCreatedAtDesc(Users user);

    long countByUserAndReadFalse(Users user);

    Optional<Notification> findByNotificationIdAndUser(UUID notificationId, Users user);

    void deleteByUser(Users user);
}
