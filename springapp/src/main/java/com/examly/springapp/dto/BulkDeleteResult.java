package com.examly.springapp.dto;

import lombok.*;

import java.util.List;

/**
 * Response for bulk-delete operations.
 * Reports which IDs were deleted and which could not be deleted (with reasons).
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BulkDeleteResult {

    private int deletedCount;
    private int failedCount;
    private List<Long> deletedIds;
    private List<FailedEntry> failed;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class FailedEntry {
        private Long id;
        private String reason;
    }
}
