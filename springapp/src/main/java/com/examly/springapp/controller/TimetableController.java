package com.examly.springapp.controller;

import com.examly.springapp.dto.TimetableRequest;
import com.examly.springapp.model.Timetable;
import com.examly.springapp.service.TimetableService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/timetables")
@RequiredArgsConstructor
public class TimetableController {

    private final TimetableService timetableService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','PRINCIPAL')")
    public ResponseEntity<Timetable> createEntry(@Valid @RequestBody TimetableRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(timetableService.createTimetableEntry(request));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Timetable>> getAll() {
        return ResponseEntity.ok(timetableService.getAllTimetables());
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Timetable> getById(@PathVariable Long id) {
        return ResponseEntity.ok(timetableService.getTimetableById(id));
    }

    @GetMapping("/section/{sectionId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Timetable>> getBySection(@PathVariable Long sectionId) {
        return ResponseEntity.ok(timetableService.getTimetableBySection(sectionId));
    }

    @GetMapping("/teacher/{teacherId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Timetable>> getByTeacher(@PathVariable Long teacherId) {
        return ResponseEntity.ok(timetableService.getTimetableByTeacher(teacherId));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','PRINCIPAL')")
    public ResponseEntity<Timetable> updateEntry(@PathVariable Long id, @Valid @RequestBody TimetableRequest request) {
        return ResponseEntity.ok(timetableService.updateTimetableEntry(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','PRINCIPAL')")
    public ResponseEntity<Void> deleteEntry(@PathVariable Long id) {
        timetableService.deleteTimetableEntry(id);
        return ResponseEntity.noContent().build();
    }
}
