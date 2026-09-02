package com.examly.springapp.controller;

import com.examly.springapp.model.*;
import com.examly.springapp.repository.*;
import com.examly.springapp.service.AttendanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * MIS Reporting endpoints (FR8).
 * Generates CSV/text reports from real database data.
 */
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','PRINCIPAL','CLASS_TEACHER')")
public class ReportController {

    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final AttendanceRepository attendanceRepository;
    private final TimetableRepository timetableRepository;
    private final ClassSectionRepository classSectionRepository;
    private final LeaveApplicationRepository leaveApplicationRepository;
    private final AttendanceService attendanceService;

    /**
     * GET /api/reports/attendance/below75
     * CSV report of students below 75% attendance.
     */
    @GetMapping(value = "/attendance/below75", produces = "text/csv")
    public ResponseEntity<byte[]> below75Report() {
        List<Map<String, Object>> below75 = attendanceService.getBelow75List();

        StringBuilder csv = new StringBuilder("Student ID,Name,Admission No,Section,Total Days,Present Days,Attendance %\n");
        for (Map<String, Object> row : below75) {
            Long studentId = (Long) row.get("studentId");
            studentRepository.findById(studentId).ifPresent(s -> {
                csv.append(s.getStudentId()).append(",")
                        .append(escapeCsv(s.getName())).append(",")
                        .append(escapeCsv(s.getAdmissionNumber())).append(",")
                        .append(s.getSection() != null ? escapeCsv(s.getSection().getSectionName()) : "").append(",")
                        .append(row.get("totalRecordedDays")).append(",")
                        .append(row.get("presentDays")).append(",")
                        .append(row.get("attendancePercentage")).append("\n");
            });
        }

        return csvResponse(csv.toString(), "below75_attendance_" + LocalDate.now() + ".csv");
    }

    /**
     * GET /api/reports/attendance/summary
     * Full attendance summary for all students as CSV.
     */
    @GetMapping(value = "/attendance/summary", produces = "text/csv")
    public ResponseEntity<byte[]> attendanceSummary() {
        List<Student> students = studentRepository.findAll();
        StringBuilder csv = new StringBuilder("Student ID,Name,Admission No,Section,Total Days,Present Days,Absent Days,Late Days,Attendance %,Status\n");

        for (Student s : students) {
            Long id = s.getStudentId();
            long total = attendanceRepository.countByStudent_StudentId(id);
            long present = attendanceRepository.countByStudent_StudentIdAndStatus(id, AttendanceStatus.PRESENT);
            long late = attendanceRepository.countByStudent_StudentIdAndStatus(id, AttendanceStatus.LATE);
            long absent = attendanceRepository.countByStudent_StudentIdAndStatus(id, AttendanceStatus.ABSENT);
            double pct = total == 0 ? 0 : Math.round(((present + late) * 100.0 / total) * 100.0) / 100.0;
            String status = pct < 75 ? "BELOW THRESHOLD" : "OK";

            csv.append(id).append(",")
                    .append(escapeCsv(s.getName())).append(",")
                    .append(escapeCsv(s.getAdmissionNumber())).append(",")
                    .append(s.getSection() != null ? escapeCsv(s.getSection().getSectionName()) : "").append(",")
                    .append(total).append(",")
                    .append(present).append(",")
                    .append(absent).append(",")
                    .append(late).append(",")
                    .append(pct).append(",")
                    .append(status).append("\n");
        }

        return csvResponse(csv.toString(), "attendance_summary_" + LocalDate.now() + ".csv");
    }

    /**
     * GET /api/reports/teacher/deployment
     * Shows which teacher is assigned to which subjects/sections.
     */
    @GetMapping(value = "/teacher/deployment", produces = "text/csv")
    public ResponseEntity<byte[]> teacherDeployment() {
        List<Timetable> entries = timetableRepository.findAll().stream()
                .filter(t -> Boolean.TRUE.equals(t.getIsActive()))
                .collect(Collectors.toList());

        StringBuilder csv = new StringBuilder("Teacher Name,Employee ID,Subject,Section,Day,Period\n");
        for (Timetable t : entries) {
            csv.append(escapeCsv(t.getTeacher().getName())).append(",")
                    .append(escapeCsv(t.getTeacher().getEmployeeId())).append(",")
                    .append(escapeCsv(t.getSubject().getSubjectName())).append(",")
                    .append(escapeCsv(t.getSection().getSectionName())).append(",")
                    .append(t.getDay()).append(",")
                    .append(t.getPeriod()).append("\n");
        }

        return csvResponse(csv.toString(), "teacher_deployment_" + LocalDate.now() + ".csv");
    }

    /**
     * GET /api/reports/leave/summary
     * Leave application summary by teacher.
     */
    @GetMapping(value = "/leave/summary", produces = "text/csv")
    public ResponseEntity<byte[]> leaveSummary() {
        List<LeaveApplication> leaves = leaveApplicationRepository.findAll();
        StringBuilder csv = new StringBuilder("Teacher Name,Employee ID,Leave Type,From,To,Status,Substitute\n");

        for (LeaveApplication l : leaves) {
            csv.append(escapeCsv(l.getTeacher().getName())).append(",")
                    .append(escapeCsv(l.getTeacher().getEmployeeId())).append(",")
                    .append(l.getLeaveType()).append(",")
                    .append(l.getFromDate()).append(",")
                    .append(l.getToDate()).append(",")
                    .append(l.getStatus()).append(",")
                    .append(l.getSubstituteTeacher() != null ? escapeCsv(l.getSubstituteTeacher().getName()) : "").append("\n");
        }

        return csvResponse(csv.toString(), "leave_summary_" + LocalDate.now() + ".csv");
    }

    /**
     * GET /api/reports/timetable
     * Full timetable export as CSV.
     */
    @GetMapping(value = "/timetable", produces = "text/csv")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> timetableExport() {
        List<Timetable> entries = timetableRepository.findAll().stream()
                .filter(t -> Boolean.TRUE.equals(t.getIsActive()))
                .sorted(Comparator.comparing(t -> t.getSection().getSectionName()))
                .collect(Collectors.toList());

        StringBuilder csv = new StringBuilder("Section,Day,Period,Subject,Teacher,Room\n");
        for (Timetable t : entries) {
            csv.append(escapeCsv(t.getSection().getSectionName())).append(",")
                    .append(t.getDay()).append(",")
                    .append(t.getPeriod()).append(",")
                    .append(escapeCsv(t.getSubject().getSubjectName())).append(",")
                    .append(escapeCsv(t.getTeacher().getName())).append(",")
                    .append(t.getRoom() != null ? escapeCsv(t.getRoom().getRoomName()) : "").append("\n");
        }

        return csvResponse(csv.toString(), "timetable_" + LocalDate.now() + ".csv");
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private ResponseEntity<byte[]> csvResponse(String csv, String filename) {
        byte[] bytes = csv.getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .contentLength(bytes.length)
                .body(bytes);
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
