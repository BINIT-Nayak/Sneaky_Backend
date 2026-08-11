package com.sneaky.sneaky.services.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.sneaky.sneaky.entity.Cart;
import com.sneaky.sneaky.entity.Products;
import com.sneaky.sneaky.entity.Users;

@Service
public class MailCartReminderEmailService implements CartReminderEmailService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MailCartReminderEmailService.class);

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final String fromAddress;
    private final String smtpHost;

    public MailCartReminderEmailService(
            ObjectProvider<JavaMailSender> mailSenderProvider,
            @Value("${app.cart-reminders.email.from}") String fromAddress,
            @Value("${spring.mail.host:}") String smtpHost) {
        this.mailSenderProvider = mailSenderProvider;
        this.fromAddress = fromAddress;
        this.smtpHost = smtpHost;
    }

    @Override
    public void sendReminder(Cart cart) {
        Users user = cart.getUser();
        Products product = cart.getProduct();
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();

        if (!StringUtils.hasText(smtpHost) || mailSender == null || !StringUtils.hasText(user.getEmail())) {
            LOGGER.info("Skipping cart reminder email for cart {} because mail is not configured or email is missing",
                    cart.getCartId());
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(user.getEmail());
        message.setSubject("Your Sneaky cart is waiting");
        message.setText("""
                Hi %s,

                %s has been in your cart for more than 2 days.
                Come back to Sneaky whenever you are ready to finish checking it out.
                """.formatted(user.getName() == null ? "there" : user.getName(), product.getName()));

        mailSender.send(message);
    }
}
