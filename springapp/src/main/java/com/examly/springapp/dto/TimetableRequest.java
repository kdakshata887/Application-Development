package com.examly.springapp.dto;

import com.examly.springapp.model.Day;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class TimetableRequest {

    @NotNull(message = "Section id is required")
    private Long sectionId;

    @NotNull(message = "Day is required")
    private Day day;

    @NotNull(message = "Period is required")
    @Min(value = 1, message = "Period must be between 1 and 8")
    @Max(value = 8, message = "Period must be between 1 and 8")
    private Integer period;

    @NotNull(message = "Subject id is required")
    private Long subjectId;

    @NotNull(message = "Teacher id is required")
    private Long teacherId;

    private Long roomId;

    private LocalDate effectiveFrom;
}
