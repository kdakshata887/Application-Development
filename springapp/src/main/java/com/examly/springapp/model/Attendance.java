package com.examly.springapp.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Represents a single attendance record.
 *
 * SCHEMA FIX: Added period, subject, and subjectId fields to enable period-wise
 * attendance tracking. The unique constraint is now (student_id, date, period)
 * — one record per student per period per day.
 *
 * If period is null, it represents a day-level (legacy) record.
 */
@Entity
@Table(name = "attendance",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_attendance_student_date_period",
                columnNames = {"student_id", "date", "period"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Attendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long attendanceId;

    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(nullable = false)
    private LocalDate date;

    /**
     * Period number (1-8). When populated, attendance is period-wise.
     * Null means legacy day-level attendance.
     */
    private Integer period;

    /**
     * The subject being attended. Links to the timetable slot.
     */
    @ManyToOne
    @JoinColumn(name = "subject_id")
    private Subject subject;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AttendanceStatus status;

    @Enumerated(EnumType.STRING)
    private CapturedBy capturedBy;

    @ManyToOne
    @JoinColumn(name = "recorded_by", referencedColumnName = "userId")
    private User recordedBy;

    private String modificationReason;

    private LocalDateTime modifiedAt;
}
