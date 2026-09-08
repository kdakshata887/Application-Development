package com.examly.springapp.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.util.List;

/**
 * Generic request body for bulk-delete operations.
 * Used by all five Admin bulk-delete endpoints.
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BulkDeleteRequest {

    @NotEmpty(message = "At least one ID must be provided for deletion")
    private List<Long> ids;
}
