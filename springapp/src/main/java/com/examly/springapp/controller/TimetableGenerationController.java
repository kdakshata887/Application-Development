package com.examly.springapp.controller;

import com.examly.springapp.dto.TimetableGenerationResult;
import com.examly.springapp.service.TimetableGenerationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/timetables")
@RequiredArgsConstructor
public class TimetableGenerationController {

    private final TimetableGenerationService generationService;

    /**
     * POST /api/timetables/generate
     * Generate timetables for ALL class sections.
     *
     * Query params:
     *   workingDays  — comma-separated (default: MON,TUE,WED,THU,FRI)
     *   periodsPerDay — integer 1–8 (default: 8)
     */
    @PostMapping("/generate")
    @PreAuthorize("hasAnyRole('ADMIN','PRINCIPAL')")
    public ResponseEntity<TimetableGenerationResult> generateAll(
            @RequestParam(defaultValue = "MON,TUE,WED,THU,FRI") String workingDays,
            @RequestParam(defaultValue = "8") int periodsPerDay) {
        TimetableGenerationResult result = generationService.generateForAll(workingDays, periodsPerDay);
        return ResponseEntity.ok(result);
    }

    /**
     * POST /api/timetables/generate/{sectionId}
     * Generate timetable for a specific class section.
     */
    @PostMapping("/generate/{sectionId}")
    @PreAuthorize("hasAnyRole('ADMIN','PRINCIPAL')")
    public ResponseEntity<TimetableGenerationResult> generateForSection(
            @PathVariable Long sectionId,
            @RequestParam(defaultValue = "MON,TUE,WED,THU,FRI") String workingDays,
            @RequestParam(defaultValue = "8") int periodsPerDay) {
        TimetableGenerationResult result = generationService.generateForSection(sectionId, workingDays, periodsPerDay);
        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/timetables/clashes
     * Returns a list of all active timetable clashes.
     */
    @GetMapping("/clashes")
    @PreAuthorize("hasAnyRole('ADMIN','PRINCIPAL','CLASS_TEACHER')")
    public ResponseEntity<Map<String, Object>> getClashReport() {
        List<String> clashes = generationService.getClashReport();
        return ResponseEntity.ok(Map.of(
                "clashCount", clashes.size(),
                "clashes", clashes,
                "hasClashes", !clashes.isEmpty()
        ));
    }
}
