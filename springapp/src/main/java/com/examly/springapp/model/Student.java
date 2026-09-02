package com.examly.springapp.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "students")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long studentId;

    @OneToOne
    @JoinColumn(name = "user_id", referencedColumnName = "userId")
    private User user;

    @Column(unique = true, nullable = false)
    private String admissionNumber;

    @Column(nullable = false)
    private String name;

    @ManyToOne
    @JoinColumn(name = "section_id")
    private ClassSection section;

    @ManyToOne
    @JoinColumn(name = "parent_id", referencedColumnName = "userId")
    private User parent;

    private String biometricDeviceRef;

    private LocalDate dateOfBirth;

    @Builder.Default
    private Boolean isActive = true;
}
