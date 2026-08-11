package com.sneaky.sneaky.dto.notification;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class NotificationDTO {
    private UUID notificationId;
    private UUID productId;
    private String productName;
    private String title;
    private String message;
    private String type;
    private Boolean read;
    private LocalDateTime createdAt;
    private LocalDateTime readAt;
}
