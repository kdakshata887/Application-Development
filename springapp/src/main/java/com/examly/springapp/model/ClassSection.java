package com.examly.springapp.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "class_sections")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ClassSection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long sectionId;

    @Column(nullable = false)
    private String className;

    @Column(nullable = false)
    private String sectionName;

    @ManyToOne
    @JoinColumn(name = "class_teacher_id")
    private Teacher classTeacher;
}
