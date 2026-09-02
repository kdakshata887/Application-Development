package com.examly.springapp.controller;

import com.examly.springapp.exception.ResourceNotFoundException;
import com.examly.springapp.model.Student;
import com.examly.springapp.repository.AttendanceRepository;
import com.examly.springapp.repository.NotificationRepository;
import com.examly.springapp.repository.StudentRepository;
import com.examly.springapp.security.UserPrincipal;
import com.examly.springapp.service.AttendanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Parent-facing API (FR7).
 * IMPORTANT: All endpoints enforce that a parent can ONLY access their own children.
 * Backend enforcement — not relying on frontend-only restrictions.
 */
@RestController
@RequestMapping("/api/parent")
@RequiredArgsConstructor
@PreAuthorize("hasRole('PARENT')")
public class ParentController {

    private final StudentRepository studentRepository;
    private final AttendanceService attendanceService;
    private final NotificationRepository notificationRepository;

    /**
     * GET /api/parent/children
     * Returns the list of children belonging to the authenticated parent.
     */
    @GetMapping("/children")
    public ResponseEntity<List<Student>> getMyChildren(@AuthenticationPrincipal UserPrincipal principal) {
        List<Student> children = studentRepository.findByParent_UserId(principal.getUserId());
        return ResponseEntity.ok(children);
    }

    /**
     * GET /api/parent/children/{studentId}/attendance
     * Returns attendance records for the authenticated parent's specific child.
     */
    @GetMapping("/children/{studentId}/attendance")
    public ResponseEntity<?> getChildAttendance(@PathVariable Long studentId,
                                                 @AuthenticationPrincipal UserPrincipal principal) {
        verifyOwnership(studentId, principal.getUserId());
        return ResponseEntity.ok(attendanceService.getAttendanceByStudent(studentId));
    }

    /**
     * GET /api/parent/children/{studentId}/percentage
     * Returns attendance percentage for the authenticated parent's specific child.
     */
    @GetMapping("/children/{studentId}/percentage")
    public ResponseEntity<Map<String, Object>> getChildAttendancePercentage(
            @PathVariable Long studentId,
            @AuthenticationPrincipal UserPrincipal principal) {
        verifyOwnership(studentId, principal.getUserId());
        return ResponseEntity.ok(attendanceService.getAttendancePercentage(studentId));
    }

    /**
     * GET /api/parent/notifications
     * Returns in-app notifications for the authenticated parent.
     */
    @GetMapping("/notifications")
    public ResponseEntity<?> getNotifications(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(
                notificationRepository.findByRecipient_UserIdOrderBySentAtDesc(principal.getUserId()));
    }

    /**
     * Enforces parent-to-child ownership.
     * Throws AccessDeniedException if the student does not belong to this parent.
     */
    private void verifyOwnership(Long studentId, Long parentUserId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found: " + studentId));
        if (student.getParent() == null || !student.getParent().getUserId().equals(parentUserId)) {
            throw new AccessDeniedException(
                    "You do not have permission to access this student's data");
        }
    }
}
