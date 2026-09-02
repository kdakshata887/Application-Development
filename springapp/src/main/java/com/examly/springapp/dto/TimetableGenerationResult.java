package com.examly.springapp.dto;

import lombok.*;
import java.util.List;

/**
 * Response from the automatic timetable generation endpoint.
 * Contains either the generated entries or a list of constraint violations.
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TimetableGenerationResult {

    private boolean success;
    private int entriesCreated;
    private String message;
    private List<String> violations;   // Non-empty when constraints cannot be satisfied
    private List<String> warnings;     // Soft-constraint warnings
}
