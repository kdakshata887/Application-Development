package com.examly.springapp.service.impl;

import com.examly.springapp.exception.ResourceNotFoundException;
import com.examly.springapp.model.*;
import com.examly.springapp.repository.NotificationRepository;
import com.examly.springapp.repository.UserRepository;
import com.examly.springapp.service.NotificationProvider;
import com.examly.springapp.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final NotificationProvider notificationProvider;

    @Override
    public Notification sendNotification(Notification notification) {
        notification.setSentAt(LocalDateTime.now());

        // Resolve recipient contact details for dispatch
        User recipient = notification.getRecipient();
        String email = recipient != null ? recipient.getEmail() : null;
        String phone = recipient != null ? recipient.getMobileNumber() : null;
        String channel = notification.getChannel() != null ? notification.getChannel().name() : "IN_APP";
        String message = notification.getMessage();
        String subject = notification.getNotificationType() != null
                ? notification.getNotificationType().name() : "Notification";

        // Dispatch via configured provider (mock by default)
        try {
            notificationProvider.dispatch(email, phone, subject, message, channel);
            notification.setStatus(NotificationStatus.SENT);
        } catch (Exception e) {
            log.error("Notification dispatch failed: {}", e.getMessage());
            notification.setStatus(NotificationStatus.PENDING);
        }

        return notificationRepository.save(notification);
    }

    /**
     * Convenience method for internal notification triggers.
     */
    public Notification notifyUser(Long userId, NotificationType type, String message, Channel channel) {
        User recipient = userRepository.findById(userId).orElse(null);
        if (recipient == null) {
            log.warn("Cannot send notification — user not found: {}", userId);
            return null;
        }
        Notification n = Notification.builder()
                .recipient(recipient)
                .notificationType(type)
                .message(message)
                .channel(channel)
                .build();
        return sendNotification(n);
    }

    @Override
    public List<Notification> getNotificationsForUser(Long userId) {
        return notificationRepository.findByRecipient_UserIdOrderBySentAtDesc(userId);
    }

    @Override
    public List<Notification> getAllNotifications() {
        return notificationRepository.findAll();
    }

    @Override
    public Notification markAsRead(Long id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id: " + id));
        notification.setStatus(NotificationStatus.READ);
        return notificationRepository.save(notification);
    }
}

