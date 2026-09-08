package com.examly.springapp.controller;

import com.examly.springapp.dto.LeaveDecisionRequest;
import com.examly.springapp.dto.LeaveRequest;
import com.examly.springapp.model.LeaveApplication;
import com.examly.springapp.model.Role;
import com.examly.springapp.model.Teacher;
import com.examly.springapp.repository.TeacherRepository;
import com.examly.springapp.security.UserPrincipal;
import com.examly.springapp.service.LeaveApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/leaves")
@RequiredArgsConstructor
public class LeaveApplicationController {

    private final LeaveApplicationService leaveApplicationService;
    private final TeacherRepository teacherRepository;

    /**
     * POST /api/leaves — Apply for leave.
     *
     * SECURITY FIX: A teacher can only apply leave for themselves.
     * Admins and Principals can apply on behalf of any teacher.
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('TEACHER','CLASS_TEACHER','ADMIN','PRINCIPAL')")
    public ResponseEntity<LeaveApplication> apply(@Valid @RequestBody LeaveRequest request,
                                                   @AuthenticationPrincipal UserPrincipal principal) {
        String role = principal.getRole();

        // Teachers can only apply for themselves
        if (role.equals(Role.TEACHER.name()) || role.equals(Role.CLASS_TEACHER.name())) {
            Teacher myTeacher = teacherRepository.findByUser_UserId(principal.getUserId())
                    .orElseThrow(() -> new AccessDeniedException(
                            "No teacher profile found for the authenticated user. " +
                            "You cannot apply for leave on behalf of another teacher."));
            if (!myTeacher.getTeacherId().equals(request.getTeacherId())) {
                throw new AccessDeniedException(
                        "Teachers can only apply for leave for themselves. " +
                        "Your teacher ID is " + myTeacher.getTeacherId() +
                        ", but the request specified teacher ID " + request.getTeacherId());
            }
        }

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

    /**
     * GET /api/leaves/teacher/{teacherId}
     * Teachers can only view their own leaves; admins can view any.
     */
    @GetMapping("/teacher/{teacherId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<LeaveApplication>> getByTeacher(@PathVariable Long teacherId,
                                                                 @AuthenticationPrincipal UserPrincipal principal) {
        String role = principal.getRole();
        if (role.equals(Role.TEACHER.name()) || role.equals(Role.CLASS_TEACHER.name())) {
            Teacher myTeacher = teacherRepository.findByUser_UserId(principal.getUserId()).orElse(null);
            if (myTeacher == null || !myTeacher.getTeacherId().equals(teacherId)) {
                throw new AccessDeniedException("Teachers can only view their own leave applications");
            }
        }
        return ResponseEntity.ok(leaveApplicationService.getLeavesByTeacher(teacherId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<LeaveApplication> getById(@PathVariable Long id) {
        return ResponseEntity.ok(leaveApplicationService.getLeaveById(id));
    }

    /**
     * GET /api/leaves/substitutes?teacherId={id}&fromDate={date}&toDate={date}
     * Returns teachers available to substitute during the given date range
     * and with no timetable clashes at the absent teacher's scheduled periods.
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
