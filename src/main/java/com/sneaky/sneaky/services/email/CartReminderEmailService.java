package com.sneaky.sneaky.services.email;

import com.sneaky.sneaky.entity.Cart;

public interface CartReminderEmailService {
    void sendReminder(Cart cart);
}
