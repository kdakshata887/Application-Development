package com.examly.springapp.model;

import jakarta.persistence.*;
import lombok.*;

/**
 * Maps a teacher to a subject with the required number of
 * periods per week for a given class section.
 * Used by the automatic timetable generation algorithm.
 */
@Entity
@Table(name = "subject_teacher_mappings",
        uniqueConstraints = @UniqueConstraint(columnNames = {"section_id", "subject_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SubjectTeacherMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long mappingId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "section_id", nullable = false)
    private ClassSection section;

    @ManyToOne(optional = false)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @ManyToOne(optional = false)
    @JoinColumn(name = "teacher_id", nullable = false)
    private Teacher teacher;

    /** How many periods per week this subject needs in the timetable. */
    @Column(nullable = false)
    @Builder.Default
    private Integer periodsPerWeek = 5;

    /** Whether this subject needs double (lab) periods. */
    @Builder.Default
    private Boolean requiresDoublePeriod = false;

    @Builder.Default
    private Boolean isActive = true;
}
