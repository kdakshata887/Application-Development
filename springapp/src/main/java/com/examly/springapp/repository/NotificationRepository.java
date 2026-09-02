package com.examly.springapp.repository;

import com.examly.springapp.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByRecipient_UserIdOrderBySentAtDesc(Long userId);
}
