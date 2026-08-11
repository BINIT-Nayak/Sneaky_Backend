package com.sneaky.sneaky.controllers;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.sneaky.sneaky.dto.notification.NotificationDTO;
import com.sneaky.sneaky.dto.notification.UnreadNotificationCountDTO;
import com.sneaky.sneaky.security.CurrentUser;
import com.sneaky.sneaky.services.NotificationService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final CurrentUser currentUser;

    @GetMapping
    public List<NotificationDTO> getNotifications() {
        return notificationService.getNotifications(currentUser.getUserId());
    }

    @GetMapping("/unread-count")
    public UnreadNotificationCountDTO getUnreadCount() {
        return new UnreadNotificationCountDTO(notificationService.countUnread(currentUser.getUserId()));
    }

    @PatchMapping("/{notificationId}/read")
    public NotificationDTO markAsRead(@PathVariable UUID notificationId) {
        return notificationService.markAsRead(currentUser.getUserId(), notificationId);
    }

    @PatchMapping("/read-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markAllAsRead() {
        notificationService.markAllAsRead(currentUser.getUserId());
    }

    @DeleteMapping("/{notificationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteNotification(@PathVariable UUID notificationId) {
        notificationService.deleteNotification(currentUser.getUserId(), notificationId);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clearNotifications() {
        notificationService.clearNotifications(currentUser.getUserId());
    }
}
