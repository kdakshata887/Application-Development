package com.examly.springapp.service.impl;

import com.examly.springapp.exception.ResourceNotFoundException;
import com.examly.springapp.model.Notification;
import com.examly.springapp.model.NotificationStatus;
import com.examly.springapp.repository.NotificationRepository;
import com.examly.springapp.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;

    @Override
    public Notification sendNotification(Notification notification) {
        notification.setSentAt(LocalDateTime.now());
        notification.setStatus(NotificationStatus.SENT);
        // Real SMS/Email/Push dispatch integration point.
        return notificationRepository.save(notification);
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
        return notification;
    }
}
