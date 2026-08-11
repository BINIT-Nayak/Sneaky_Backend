package com.sneaky.sneaky.controllers;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.sneaky.sneaky.dto.notification.NotificationDTO;
import com.sneaky.sneaky.security.CurrentUser;
import com.sneaky.sneaky.services.NotificationService;

class NotificationControllerTest {

    private final NotificationService notificationService = org.mockito.Mockito.mock(NotificationService.class);
    private final CurrentUser currentUser = org.mockito.Mockito.mock(CurrentUser.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new NotificationController(notificationService, currentUser)).build();
    }

    @Test
    void listsNotificationsAndUnreadCountForCurrentUser() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID notificationId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        NotificationDTO dto = new NotificationDTO(
                notificationId,
                productId,
                "Air Max",
                "Your cart is waiting",
                "Air Max has been waiting in your cart for more than 2 days.",
                "CART_REMINDER",
                false,
                LocalDateTime.now(),
                null);

        when(currentUser.getUserId()).thenReturn(userId);
        when(notificationService.getNotifications(userId)).thenReturn(List.of(dto));
        when(notificationService.countUnread(userId)).thenReturn(1L);

        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].notificationId").value(notificationId.toString()))
                .andExpect(jsonPath("$[0].productId").value(productId.toString()))
                .andExpect(jsonPath("$[0].read").value(false));

        mockMvc.perform(get("/api/notifications/unread-count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(1));
    }

    @Test
    void supportsReadReadAllAndDeleteActions() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID notificationId = UUID.randomUUID();
        when(currentUser.getUserId()).thenReturn(userId);

        mockMvc.perform(patch("/api/notifications/{notificationId}/read", notificationId))
                .andExpect(status().isOk());
        mockMvc.perform(patch("/api/notifications/read-all"))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/notifications/{notificationId}", notificationId))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/notifications"))
                .andExpect(status().isNoContent());

        verify(notificationService).markAsRead(userId, notificationId);
        verify(notificationService).markAllAsRead(userId);
        verify(notificationService).deleteNotification(userId, notificationId);
        verify(notificationService).clearNotifications(userId);
    }
}
