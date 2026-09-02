package com.examly.springapp.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "timetable")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Timetable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long timetableId;

    @ManyToOne
    @JoinColumn(name = "section_id", nullable = false)
    private ClassSection section;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Day day;

    @Column(nullable = false)
    private Integer period; // 1-8

    @ManyToOne
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @ManyToOne
    @JoinColumn(name = "teacher_id", nullable = false)
    private Teacher teacher;

    @ManyToOne
    @JoinColumn(name = "room_id")
    private Room room;

    private LocalDate effectiveFrom;

    @Builder.Default
    private Boolean isActive = true;

    @Builder.Default
    private Integer version = 1;
}
