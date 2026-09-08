package com.examly.springapp.controller;

import com.examly.springapp.dto.AttendanceMarkRequest;
import com.examly.springapp.exception.ResourceNotFoundException;
import com.examly.springapp.model.Attendance;
import com.examly.springapp.model.Role;
import com.examly.springapp.model.Student;
import com.examly.springapp.repository.StudentRepository;
import com.examly.springapp.security.UserPrincipal;
import com.examly.springapp.service.AttendanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
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
    private final StudentRepository studentRepository;

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

    /**
     * GET /api/attendance/student/{studentId}
     *
     * IDOR FIX: Access control rules:
     * - ADMIN / PRINCIPAL / TEACHER / CLASS_TEACHER → can access any student
     * - STUDENT role → can only access their own records (userId must match the student's linked user)
     * - PARENT role → can only access their children's records
     */
    @GetMapping("/student/{studentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Attendance>> getByStudent(@PathVariable Long studentId,
                                                          @AuthenticationPrincipal UserPrincipal principal) {
        enforceStudentDataAccess(studentId, principal);
        return ResponseEntity.ok(attendanceService.getAttendanceByStudent(studentId));
    }

    @GetMapping("/student/{studentId}/range")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Attendance>> getByStudentRange(@PathVariable Long studentId,
                                                               @RequestParam LocalDate from,
                                                               @RequestParam LocalDate to,
                                                               @AuthenticationPrincipal UserPrincipal principal) {
        enforceStudentDataAccess(studentId, principal);
        return ResponseEntity.ok(attendanceService.getAttendanceByStudentAndRange(studentId, from, to));
    }

    @GetMapping("/section/{sectionId}")
    @PreAuthorize("hasAnyRole('TEACHER','CLASS_TEACHER','ADMIN','PRINCIPAL')")
    public ResponseEntity<List<Attendance>> getBySectionAndDate(@PathVariable Long sectionId,
                                                                  @RequestParam LocalDate date) {
        return ResponseEntity.ok(attendanceService.getAttendanceBySectionAndDate(sectionId, date));
    }

    /**
     * GET /api/attendance/student/{studentId}/percentage
     *
     * IDOR FIX: Same access rules as getByStudent.
     */
    @GetMapping("/student/{studentId}/percentage")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getPercentage(@PathVariable Long studentId,
                                                              @AuthenticationPrincipal UserPrincipal principal) {
        enforceStudentDataAccess(studentId, principal);
        return ResponseEntity.ok(attendanceService.getAttendancePercentage(studentId));
    }

    @GetMapping("/below-75")
    @PreAuthorize("hasAnyRole('ADMIN','PRINCIPAL','CLASS_TEACHER')")
    public ResponseEntity<List<Map<String, Object>>> getBelow75() {
        return ResponseEntity.ok(attendanceService.getBelow75List());
    }

    /**
     * Enforces ownership / relationship-based access for student data.
     * Throws AccessDeniedException if the caller is not allowed to access the requested student's data.
     */
    private void enforceStudentDataAccess(Long studentId, UserPrincipal principal) {
        String role = principal.getRole();

        // Privileged roles can access all students
        if (role.equals(Role.ADMIN.name()) || role.equals(Role.PRINCIPAL.name())
                || role.equals(Role.TEACHER.name()) || role.equals(Role.CLASS_TEACHER.name())) {
            return;
        }

        // STUDENT: can only access their own record
        if (role.equals(Role.STUDENT.name())) {
            Student student = studentRepository.findById(studentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Student not found: " + studentId));
            if (!student.getUser().getUserId().equals(principal.getUserId())) {
                throw new AccessDeniedException("Students can only access their own attendance records");
            }
            return;
        }

        // PARENT: can only access their children's records
        if (role.equals(Role.PARENT.name())) {
            Student student = studentRepository.findById(studentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Student not found: " + studentId));
            if (student.getParent() == null || !student.getParent().getUserId().equals(principal.getUserId())) {
                throw new AccessDeniedException("Parents can only access their own children's attendance records");
            }
            return;
        }

        throw new AccessDeniedException("Access denied");
    }
}
