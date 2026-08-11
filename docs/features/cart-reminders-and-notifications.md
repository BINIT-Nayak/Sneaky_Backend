# Cart Reminders And Notifications

Sneaky checks once per day for products that have stayed in a cart for more than two days. Each eligible cart item receives one in-app notification and, when SMTP is configured, one email reminder. Adding the same product to the cart again resets the two-day reminder window.

The reminder job is enabled through Spring scheduling and can be disabled with:

```bash
APP_CART_REMINDERS_ENABLED=false
```

Cart rows keep a reminder timestamp so the same cart item is not reminded repeatedly until it is added/reset again.

## Notification API

Notification endpoints require authentication:

```http
GET /api/notifications
GET /api/notifications/unread-count
PATCH /api/notifications/{notificationId}/read
PATCH /api/notifications/read-all
DELETE /api/notifications/{notificationId}
DELETE /api/notifications
```

## Configuration

Configure the worker and SMTP connection with the `APP_CART_REMINDERS_*` and `SPRING_MAIL_*` variables shown in `.env.example`.

```bash
APP_CART_REMINDERS_ENABLED=true
APP_CART_REMINDERS_AGE_DAYS=2
APP_CART_REMINDERS_CRON=0 0 9 * * *
APP_CART_REMINDERS_EMAIL_FROM=no-reply@example.com

SPRING_MAIL_HOST=smtp.example.com
SPRING_MAIL_PORT=587
SPRING_MAIL_USERNAME=
SPRING_MAIL_PASSWORD=
SPRING_MAIL_SMTP_AUTH=true
SPRING_MAIL_SMTP_STARTTLS_ENABLE=true
```

The default reminder schedule is daily at 09:00 server time.

## Runtime Notes

- The feature adds a `notifications` table and cart reminder metadata. Keep `SPRING_JPA_HIBERNATE_DDL_AUTO=update` for automatic schema updates, or add equivalent database migrations before deployment.
- In-app notifications are created even if email sending fails.
- SMTP settings are optional for local development, but required for production email reminders.
