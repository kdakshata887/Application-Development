package com.examly.springapp.dto;

import com.examly.springapp.model.AttendanceStatus;
import com.examly.springapp.model.CapturedBy;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AttendanceMarkRequest {

    @NotNull(message = "Student id is required")
    private Long studentId;

    @NotNull(message = "Date is required")
    private LocalDate date;

    @NotNull(message = "Status is required")
    private AttendanceStatus status;

    private CapturedBy capturedBy;

    private String modificationReason;
}
