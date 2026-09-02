package com.examly.springapp.controller;

import com.examly.springapp.dto.AttendanceMarkRequest;
import com.examly.springapp.model.Attendance;
import com.examly.springapp.security.UserPrincipal;
import com.examly.springapp.service.AttendanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;

    @PostMapping("/mark")
    @PreAuthorize("hasAnyRole('TEACHER','CLASS_TEACHER','ADMIN','PRINCIPAL')")
    public ResponseEntity<Attendance> markAttendance(@Valid @RequestBody AttendanceMarkRequest request,
                                                       @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(attendanceService.markAttendance(request, principal.getUserId()));
    }

    @PutMapping("/{id}/override")
    @PreAuthorize("hasAnyRole('CLASS_TEACHER','ADMIN','PRINCIPAL')")
    public ResponseEntity<Attendance> overrideAttendance(@PathVariable Long id,
                                                           @Valid @RequestBody AttendanceMarkRequest request,
                                                           @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(attendanceService.overrideAttendance(id, request, principal.getUserId()));
    }

    @GetMapping("/student/{studentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Attendance>> getByStudent(@PathVariable Long studentId) {
        return ResponseEntity.ok(attendanceService.getAttendanceByStudent(studentId));
    }

    @GetMapping("/student/{studentId}/range")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Attendance>> getByStudentRange(@PathVariable Long studentId,
                                                                @RequestParam LocalDate from,
                                                                @RequestParam LocalDate to) {
        return ResponseEntity.ok(attendanceService.getAttendanceByStudentAndRange(studentId, from, to));
    }

    @GetMapping("/section/{sectionId}")
    @PreAuthorize("hasAnyRole('TEACHER','CLASS_TEACHER','ADMIN','PRINCIPAL')")
    public ResponseEntity<List<Attendance>> getBySectionAndDate(@PathVariable Long sectionId,
                                                                  @RequestParam LocalDate date) {
        return ResponseEntity.ok(attendanceService.getAttendanceBySectionAndDate(sectionId, date));
    }

    @GetMapping("/student/{studentId}/percentage")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getPercentage(@PathVariable Long studentId) {
        return ResponseEntity.ok(attendanceService.getAttendancePercentage(studentId));
    }

    @GetMapping("/below-75")
    @PreAuthorize("hasAnyRole('ADMIN','PRINCIPAL','CLASS_TEACHER')")
    public ResponseEntity<List<Map<String, Object>>> getBelow75() {
        return ResponseEntity.ok(attendanceService.getBelow75List());
    }
}
