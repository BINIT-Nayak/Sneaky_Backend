package com.sneaky.sneaky.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sneaky.sneaky.entity.Cart;
import com.sneaky.sneaky.entity.Products;
import com.sneaky.sneaky.entity.Users;
import com.sneaky.sneaky.repository.CartRepository;
import com.sneaky.sneaky.services.email.CartReminderEmailService;

@ExtendWith(MockitoExtension.class)
class CartReminderServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private CartReminderEmailService emailService;

    private CartReminderService cartReminderService;

    @BeforeEach
    void setUp() {
        cartReminderService = new CartReminderService(cartRepository, notificationService, emailService);
        ReflectionTestUtils.setField(cartReminderService, "remindersEnabled", true);
        ReflectionTestUtils.setField(cartReminderService, "reminderAgeDays", 2L);
    }

    @Test
    void createsNotificationSendsEmailAndMarksCandidateAsSent() {
        Cart cart = cart();
        when(cartRepository.findReminderCandidates(any(LocalDateTime.class))).thenReturn(List.of(cart));

        cartReminderService.sendCartReminders();

        verify(notificationService).createCartReminder(cart.getUser(), cart.getProduct());
        verify(emailService).sendReminder(cart);
        verify(cartRepository).save(cart);
        assertThat(cart.getReminderSentAt()).isNotNull();
    }

    @Test
    void disabledReminderJobDoesNothing() {
        ReflectionTestUtils.setField(cartReminderService, "remindersEnabled", false);

        cartReminderService.sendCartReminders();

        verify(cartRepository, never()).findReminderCandidates(any());
    }

    @Test
    void emailFailureStillKeepsInAppNotificationAndSentMarker() {
        Cart cart = cart();
        when(cartRepository.findReminderCandidates(any(LocalDateTime.class))).thenReturn(List.of(cart));
        org.mockito.Mockito.doThrow(new IllegalStateException("SMTP unavailable"))
                .when(emailService)
                .sendReminder(cart);

        cartReminderService.sendCartReminders();

        verify(notificationService).createCartReminder(cart.getUser(), cart.getProduct());
        verify(cartRepository).save(cart);
        assertThat(cart.getReminderSentAt()).isNotNull();
    }

    private static Cart cart() {
        Users user = new Users();
        user.setUserId(UUID.randomUUID());
        user.setName("Sneaky User");
        user.setEmail("user@sneaky.test");

        Products product = new Products();
        product.setProductId(UUID.randomUUID());
        product.setName("Air Max");
        product.setPrice(BigDecimal.valueOf(12999));

        return Cart.builder()
                .cartId(1L)
                .user(user)
                .product(product)
                .quantity(1)
                .createdAt(LocalDateTime.now().minusDays(3))
                .build();
    }
}
