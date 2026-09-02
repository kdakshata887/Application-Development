package com.examly.springapp.service;

/**
 * Provider interface for dispatching notifications.
 * Implementations:
 *   - MockNotificationProvider  — logs to SLF4J (default, no external service needed)
 *   - EmailNotificationProvider — dispatches via Spring Mail (requires mail config)
 *
 * Configure via: app.notification.provider=mock|email
 */
public interface NotificationProvider {

    /**
     * Send a notification to a recipient.
     *
     * @param recipientEmail the target email address (may be null for in-app only)
     * @param recipientPhone the target phone number (may be null)
     * @param subject        the notification subject/title
     * @param message        the notification body
     * @param channel        the channel hint (EMAIL, SMS, PUSH, IN_APP)
     */
    void dispatch(String recipientEmail, String recipientPhone, String subject, String message, String channel);
}
