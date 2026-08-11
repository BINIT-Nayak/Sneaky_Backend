package com.sneaky.sneaky.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.sneaky.sneaky.entity.Notification;
import com.sneaky.sneaky.entity.Products;
import com.sneaky.sneaky.entity.Users;
import com.sneaky.sneaky.repository.NotificationRepository;
import com.sneaky.sneaky.repository.UsersRepository;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UsersRepository usersRepository;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    void listsNotificationsNewestFirstUsingRepositoryContract() {
        Users user = user();
        Products product = product();
        Notification notification = notification(user, product);

        when(usersRepository.findById(user.getUserId())).thenReturn(Optional.of(user));
        when(notificationRepository.findByUserOrderByCreatedAtDesc(user)).thenReturn(List.of(notification));

        assertThat(notificationService.getNotifications(user.getUserId())).singleElement().satisfies(dto -> {
            assertThat(dto.getProductId()).isEqualTo(product.getProductId());
            assertThat(dto.getProductName()).isEqualTo("Air Max");
            assertThat(dto.getType()).isEqualTo(NotificationService.CART_REMINDER_TYPE);
            assertThat(dto.getRead()).isFalse();
        });
    }

    @Test
    void marksOwnedNotificationAsRead() {
        Users user = user();
        Notification notification = notification(user, product());

        when(usersRepository.findById(user.getUserId())).thenReturn(Optional.of(user));
        when(notificationRepository.findByNotificationIdAndUser(notification.getNotificationId(), user))
                .thenReturn(Optional.of(notification));
        when(notificationRepository.save(notification)).thenReturn(notification);

        notificationService.markAsRead(user.getUserId(), notification.getNotificationId());

        assertThat(notification.getRead()).isTrue();
        assertThat(notification.getReadAt()).isNotNull();
        verify(notificationRepository).save(notification);
    }

    @Test
    void rejectsNotificationOwnedByAnotherUser() {
        Users user = user();
        UUID notificationId = UUID.randomUUID();

        when(usersRepository.findById(user.getUserId())).thenReturn(Optional.of(user));
        when(notificationRepository.findByNotificationIdAndUser(notificationId, user)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.markAsRead(user.getUserId(), notificationId))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode")
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    private static Users user() {
        Users user = new Users();
        user.setUserId(UUID.randomUUID());
        user.setEmail("user@sneaky.test");
        return user;
    }

    private static Products product() {
        Products product = new Products();
        product.setProductId(UUID.randomUUID());
        product.setName("Air Max");
        return product;
    }

    private static Notification notification(Users user, Products product) {
        return Notification.builder()
                .notificationId(UUID.randomUUID())
                .user(user)
                .product(product)
                .title("Still thinking about Air Max?")
                .message("Air Max has been waiting in your cart for more than 2 days.")
                .type(NotificationService.CART_REMINDER_TYPE)
                .read(false)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
