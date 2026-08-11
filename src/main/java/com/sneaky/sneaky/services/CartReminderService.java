package com.sneaky.sneaky.services;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.sneaky.sneaky.entity.Cart;
import com.sneaky.sneaky.repository.CartRepository;
import com.sneaky.sneaky.services.email.CartReminderEmailService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CartReminderService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CartReminderService.class);

    private final CartRepository cartRepository;
    private final NotificationService notificationService;
    private final CartReminderEmailService emailService;

    @Value("${app.cart-reminders.enabled:true}")
    private boolean remindersEnabled;

    @Value("${app.cart-reminders.age-days:2}")
    private long reminderAgeDays;

    @Scheduled(cron = "${app.cart-reminders.cron}")
    public void sendCartReminders() {
        if (!remindersEnabled) {
            return;
        }

        LocalDateTime cutoff = LocalDateTime.now().minusDays(reminderAgeDays);
        List<Cart> candidates = cartRepository.findReminderCandidates(cutoff);

        candidates.forEach(cart -> {
            try {
                notificationService.createCartReminder(cart.getUser(), cart.getProduct());
                cart.setReminderSentAt(LocalDateTime.now());
                cartRepository.save(cart);
            } catch (RuntimeException e) {
                LOGGER.warn("Failed to create cart reminder notification for cart {}", cart.getCartId(), e);
                return;
            }

            try {
                emailService.sendReminder(cart);
            } catch (RuntimeException e) {
                LOGGER.warn("Failed to send cart reminder email for cart {}", cart.getCartId(), e);
            }
        });
    }
}
