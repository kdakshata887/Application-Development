package com.examly.springapp.service;

import com.examly.springapp.model.Notification;

import java.util.List;

public interface NotificationService {
    Notification sendNotification(Notification notification);
    List<Notification> getNotificationsForUser(Long userId);
    List<Notification> getAllNotifications();
    Notification markAsRead(Long id);
}
