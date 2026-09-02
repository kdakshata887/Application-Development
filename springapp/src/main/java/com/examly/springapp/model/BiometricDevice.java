package com.examly.springapp.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "biometric_devices")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BiometricDevice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long deviceId;

    @Column(nullable = false)
    private String deviceName;

    private String location;

    @Enumerated(EnumType.STRING)
    private DeviceType deviceType;

    private LocalDateTime lastSync;

    private Integer batteryLevel;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private DeviceStatus status = DeviceStatus.OFFLINE;
}
