package com.examly.springapp.service.impl;

import com.examly.springapp.service.NotificationProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Mock notification provider — logs all notifications to SLF4J.
 * No external service credentials are required.
 * Active when app.notification.provider=mock (the default).
 *
 * To enable real email dispatch, set app.notification.provider=email
 * and configure the mail properties.
 */
@Component
@ConditionalOnProperty(name = "app.notification.provider", havingValue = "mock", matchIfMissing = true)
@Slf4j
public class MockNotificationProvider implements NotificationProvider {

    @Override
    public void dispatch(String recipientEmail, String recipientPhone, String subject, String message, String channel) {
        log.info("[MOCK NOTIFICATION] Channel={} | To={} / {} | Subject={} | Message={}",
                channel,
                recipientEmail != null ? recipientEmail : "(no email)",
                recipientPhone != null ? recipientPhone : "(no phone)",
                subject,
                message);
    }
}
