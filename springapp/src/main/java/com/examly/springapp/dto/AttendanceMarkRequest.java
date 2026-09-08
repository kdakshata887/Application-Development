package com.examly.springapp.dto;

import com.examly.springapp.model.AttendanceStatus;
import com.examly.springapp.model.CapturedBy;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;

/**
 * Request DTO for marking or creating an attendance record.
 *
 * period and subjectId are optional for backward compatibility.
 * When provided, they enable period-wise attendance tracking.
 *
 * Note: modificationReason is ONLY required when overriding an existing record
 * (via the /override endpoint). Initial marking does not require a reason.
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AttendanceMarkRequest {

    @NotNull(message = "Student id is required")
    private Long studentId;

    @NotNull(message = "Date is required")
    private LocalDate date;

    /** Period number (1–8). Optional for day-level attendance; required for period-wise. */
    @Min(value = 1, message = "Period must be between 1 and 8")
    @Max(value = 8, message = "Period must be between 1 and 8")
    private Integer period;

    /** Subject being attended. Optional but recommended for period-wise attendance. */
    private Long subjectId;

    @NotNull(message = "Status is required")
    private AttendanceStatus status;

    private CapturedBy capturedBy;

    /** Required when overriding an existing record; optional for initial marking. */
    private String modificationReason;
}
