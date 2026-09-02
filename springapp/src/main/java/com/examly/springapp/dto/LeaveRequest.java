package com.examly.springapp.dto;

import com.examly.springapp.model.LeaveType;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class LeaveRequest {

    @NotNull(message = "Teacher id is required")
    private Long teacherId;

    @NotNull(message = "Leave type is required")
    private LeaveType leaveType;

    @NotNull(message = "From date is required")
    private LocalDate fromDate;

    @NotNull(message = "To date is required")
    private LocalDate toDate;

    private String reason;
}
