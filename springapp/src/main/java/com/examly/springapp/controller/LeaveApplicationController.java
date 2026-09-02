package com.examly.springapp.controller;

import com.examly.springapp.dto.LeaveDecisionRequest;
import com.examly.springapp.dto.LeaveRequest;
import com.examly.springapp.model.LeaveApplication;
import com.examly.springapp.model.Teacher;
import com.examly.springapp.service.LeaveApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/leaves")
@RequiredArgsConstructor
public class LeaveApplicationController {

    private final LeaveApplicationService leaveApplicationService;

    @PostMapping
    @PreAuthorize("hasAnyRole('TEACHER','CLASS_TEACHER')")
    public ResponseEntity<LeaveApplication> apply(@Valid @RequestBody LeaveRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(leaveApplicationService.applyLeave(request));
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('PRINCIPAL','ADMIN')")
    public ResponseEntity<LeaveApplication> approve(@PathVariable Long id, @RequestBody LeaveDecisionRequest request) {
        return ResponseEntity.ok(leaveApplicationService.approveLeave(id, request));
    }

    @PutMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('PRINCIPAL','ADMIN')")
    public ResponseEntity<LeaveApplication> reject(@PathVariable Long id, @RequestBody LeaveDecisionRequest request) {
        return ResponseEntity.ok(leaveApplicationService.rejectLeave(id, request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('PRINCIPAL','ADMIN')")
    public ResponseEntity<List<LeaveApplication>> getAll() {
        return ResponseEntity.ok(leaveApplicationService.getAllLeaves());
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('PRINCIPAL','ADMIN')")
    public ResponseEntity<List<LeaveApplication>> getPending() {
        return ResponseEntity.ok(leaveApplicationService.getPendingLeaves());
    }

    @GetMapping("/teacher/{teacherId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<LeaveApplication>> getByTeacher(@PathVariable Long teacherId) {
        return ResponseEntity.ok(leaveApplicationService.getLeavesByTeacher(teacherId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<LeaveApplication> getById(@PathVariable Long id) {
        return ResponseEntity.ok(leaveApplicationService.getLeaveById(id));
    }

    /**
     * GET /api/leaves/substitutes?teacherId={id}&fromDate={date}&toDate={date}
     * Returns teachers available to substitute during the given date range.
     */
    @GetMapping("/substitutes")
    @PreAuthorize("hasAnyRole('PRINCIPAL','ADMIN','TEACHER','CLASS_TEACHER')")
    public ResponseEntity<List<Teacher>> getSubstitutes(
            @RequestParam Long teacherId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        return ResponseEntity.ok(leaveApplicationService.getSuggestedSubstitutes(teacherId, fromDate, toDate));
    }
}
