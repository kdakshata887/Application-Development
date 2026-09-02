package com.examly.springapp.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long notificationId;

    @ManyToOne
    @JoinColumn(name = "recipient_user_id", referencedColumnName = "userId", nullable = false)
    private User recipient;

    @Enumerated(EnumType.STRING)
    private NotificationType notificationType;

    @Column(length = 1000)
    private String message;

    @Enumerated(EnumType.STRING)
    private Channel channel;

    private LocalDateTime sentAt;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private NotificationStatus status = NotificationStatus.PENDING;
}
