package com.sneaky.sneaky.services;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.sneaky.sneaky.dto.notification.NotificationDTO;
import com.sneaky.sneaky.entity.Notification;
import com.sneaky.sneaky.entity.Products;
import com.sneaky.sneaky.entity.Users;
import com.sneaky.sneaky.repository.NotificationRepository;
import com.sneaky.sneaky.repository.UsersRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NotificationService {

    public static final String CART_REMINDER_TYPE = "CART_REMINDER";

    private final NotificationRepository notificationRepository;
    private final UsersRepository usersRepository;

    @Transactional
    public Notification createCartReminder(Users user, Products product) {
        String productName = product.getName();
        return notificationRepository.save(Notification.builder()
                .user(user)
                .product(product)
                .title("Still thinking about " + productName + "?")
                .message(productName + " has been waiting in your cart for more than 2 days.")
                .type(CART_REMINDER_TYPE)
                .createdAt(LocalDateTime.now())
                .build());
    }

    @Transactional(readOnly = true)
    public List<NotificationDTO> getNotifications(UUID userId) {
        Users user = getUser(userId);
        return notificationRepository.findByUserOrderByCreatedAtDesc(user)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public long countUnread(UUID userId) {
        return notificationRepository.countByUserAndReadFalse(getUser(userId));
    }

    @Transactional
    public NotificationDTO markAsRead(UUID userId, UUID notificationId) {
        Notification notification = getOwnedNotification(userId, notificationId);
        if (!Boolean.TRUE.equals(notification.getRead())) {
            notification.setRead(true);
            notification.setReadAt(LocalDateTime.now());
        }
        return toDto(notificationRepository.save(notification));
    }

    @Transactional
    public void markAllAsRead(UUID userId) {
        Users user = getUser(userId);
        LocalDateTime now = LocalDateTime.now();
        notificationRepository.findByUserOrderByCreatedAtDesc(user).forEach(notification -> {
            if (!Boolean.TRUE.equals(notification.getRead())) {
                notification.setRead(true);
                notification.setReadAt(now);
            }
        });
    }

    @Transactional
    public void deleteNotification(UUID userId, UUID notificationId) {
        notificationRepository.delete(getOwnedNotification(userId, notificationId));
    }

    @Transactional
    public void clearNotifications(UUID userId) {
        notificationRepository.deleteByUser(getUser(userId));
    }

    private Notification getOwnedNotification(UUID userId, UUID notificationId) {
        Users user = getUser(userId);
        return notificationRepository.findByNotificationIdAndUser(notificationId, user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private Users getUser(UUID userId) {
        return usersRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }

    private NotificationDTO toDto(Notification notification) {
        Products product = notification.getProduct();
        return new NotificationDTO(
                notification.getNotificationId(),
                product == null ? null : product.getProductId(),
                product == null ? null : product.getName(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getType(),
                notification.getRead(),
                notification.getCreatedAt(),
                notification.getReadAt());
    }
}
