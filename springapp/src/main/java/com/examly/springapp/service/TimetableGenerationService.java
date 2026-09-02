package com.examly.springapp.service;

import com.examly.springapp.dto.TimetableGenerationResult;

/**
 * Service for automatic, constraint-based timetable generation (FR4).
 * Uses a deterministic greedy + backtracking algorithm.
 */
public interface TimetableGenerationService {

    /**
     * Generate a full timetable for a single class section.
     * Clears existing active entries for the section before generating.
     *
     * @param sectionId the section to generate for
     * @param workingDays comma-separated days, e.g. "MON,TUE,WED,THU,FRI"
     * @param periodsPerDay maximum periods per day (1–8)
     * @return result with success flag, entry count, and constraint violations if any
     */
    TimetableGenerationResult generateForSection(Long sectionId, String workingDays, int periodsPerDay);

    /**
     * Generate timetables for ALL active class sections.
     *
     * @param workingDays comma-separated working days
     * @param periodsPerDay maximum periods per day
     * @return combined result
     */
    TimetableGenerationResult generateForAll(String workingDays, int periodsPerDay);

    /**
     * Report all currently active timetable clashes.
     * Returns a list of descriptive clash messages.
     */
    java.util.List<String> getClashReport();
}
