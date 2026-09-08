package com.examly.springapp.dto;

import com.examly.springapp.model.LeaveType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LeaveRequest {

    @NotNull(message = "Teacher id is required")
    private Long teacherId;

    @NotNull(message = "Leave type is required")
    private LeaveType leaveType;

    @NotNull(message = "From date is required")
    private LocalDate fromDate;

    @NotNull(message = "To date is required")
    private LocalDate toDate;

    @NotBlank(message = "Reason is required when applying for leave")
    private String reason;
}
