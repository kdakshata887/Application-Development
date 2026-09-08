package com.examly.springapp.controller;

import com.examly.springapp.model.Notification;
import com.examly.springapp.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','PRINCIPAL','TEACHER','CLASS_TEACHER')")
    public ResponseEntity<Notification> send(@RequestBody Notification notification) {
        return ResponseEntity.status(HttpStatus.CREATED).body(notificationService.sendNotification(notification));
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Notification>> getForUser(@PathVariable Long userId) {
        return ResponseEntity.ok(notificationService.getNotificationsForUser(userId));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','PRINCIPAL')")
    public ResponseEntity<List<Notification>> getAll() {
        return ResponseEntity.ok(notificationService.getAllNotifications());
    }

    /**
     * PUT /api/notifications/{id}/read
     * Mark a notification as read.
     * FIX: This endpoint was missing — the frontend calls it but it didn't exist.
     */
    @PutMapping("/{id}/read")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Notification> markRead(@PathVariable Long id) {
        return ResponseEntity.ok(notificationService.markAsRead(id));
    }
}
