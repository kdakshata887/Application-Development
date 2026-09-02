package com.examly.springapp.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "teachers")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Teacher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long teacherId;

    @OneToOne
    @JoinColumn(name = "user_id", referencedColumnName = "userId")
    private User user;

    @Column(unique = true, nullable = false)
    private String employeeId;

    @Column(nullable = false)
    private String name;

    private String qualification;

    private String subjectSpecialization;

    private String biometricDeviceRef;

    @Builder.Default
    private Boolean isActive = true;
}
