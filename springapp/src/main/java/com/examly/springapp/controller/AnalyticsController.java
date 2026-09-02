package com.examly.springapp.controller;

import com.examly.springapp.model.*;
import com.examly.springapp.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Analytics dashboard endpoints (FR9, FR15).
 * All data is pulled from the real database — no hardcoded/fake data.
 */
@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','PRINCIPAL','CLASS_TEACHER')")
public class AnalyticsController {

    private final AttendanceRepository attendanceRepository;
    private final StudentRepository studentRepository;
    private final TimetableRepository timetableRepository;
    private final TeacherRepository teacherRepository;
    private final ClassSectionRepository classSectionRepository;

    /**
     * GET /api/analytics/attendance/overview
     * Overall attendance stats: total students, average percentage, below-75 count.
     */
    @GetMapping("/attendance/overview")
    public ResponseEntity<Map<String, Object>> attendanceOverview() {
        List<Student> students = studentRepository.findAll();
        long total = students.size();
        long below75 = 0;
        double totalPct = 0;

        for (Student s : students) {
            long totalDays = attendanceRepository.countByStudent_StudentId(s.getStudentId());
            long presentDays = attendanceRepository.countByStudent_StudentIdAndStatus(s.getStudentId(), AttendanceStatus.PRESENT)
                    + attendanceRepository.countByStudent_StudentIdAndStatus(s.getStudentId(), AttendanceStatus.LATE);
            double pct = totalDays == 0 ? 0 : (presentDays * 100.0) / totalDays;
            totalPct += pct;
            if (pct < 75) below75++;
        }

        double avgPct = total == 0 ? 0 : Math.round((totalPct / total) * 100.0) / 100.0;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalStudents", total);
        result.put("averageAttendancePercent", avgPct);
        result.put("below75Count", below75);
        result.put("above75Count", total - below75);
        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/analytics/attendance/by-section
     * Attendance breakdown per class section.
     */
    @GetMapping("/attendance/by-section")
    public ResponseEntity<List<Map<String, Object>>> attendanceBySection() {
        List<ClassSection> sections = classSectionRepository.findAll();
        List<Map<String, Object>> result = new ArrayList<>();

        for (ClassSection section : sections) {
            List<Student> students = studentRepository.findBySection_SectionId(section.getSectionId());
            long studentCount = students.size();
            double totalPct = 0;
            int counted = 0;

            for (Student s : students) {
                long totalDays = attendanceRepository.countByStudent_StudentId(s.getStudentId());
                if (totalDays == 0) continue;
                long presentDays = attendanceRepository.countByStudent_StudentIdAndStatus(s.getStudentId(), AttendanceStatus.PRESENT)
                        + attendanceRepository.countByStudent_StudentIdAndStatus(s.getStudentId(), AttendanceStatus.LATE);
                totalPct += (presentDays * 100.0) / totalDays;
                counted++;
            }

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("sectionId", section.getSectionId());
            row.put("sectionName", section.getSectionName());
            row.put("studentCount", studentCount);
            row.put("averageAttendance", counted == 0 ? 0 : Math.round((totalPct / counted) * 100.0) / 100.0);
            result.add(row);
        }
        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/analytics/attendance/by-day-of-week
     * Shows which days of the week have highest/lowest attendance.
     */
    @GetMapping("/attendance/by-day-of-week")
    public ResponseEntity<List<Map<String, Object>>> attendanceByDayOfWeek() {
        List<Attendance> all = attendanceRepository.findAll();
        Map<DayOfWeek, Long> presentMap = new LinkedHashMap<>();
        Map<DayOfWeek, Long> totalMap = new LinkedHashMap<>();

        for (DayOfWeek dow : DayOfWeek.values()) {
            presentMap.put(dow, 0L);
            totalMap.put(dow, 0L);
        }

        for (Attendance a : all) {
            DayOfWeek dow = a.getDate().getDayOfWeek();
            totalMap.merge(dow, 1L, Long::sum);
            if (a.getStatus() == AttendanceStatus.PRESENT || a.getStatus() == AttendanceStatus.LATE) {
                presentMap.merge(dow, 1L, Long::sum);
            }
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (DayOfWeek dow : DayOfWeek.values()) {
            long total = totalMap.get(dow);
            long present = presentMap.get(dow);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("dayOfWeek", dow.name());
            row.put("totalRecords", total);
            row.put("presentCount", present);
            row.put("attendancePercent", total == 0 ? 0 : Math.round((present * 100.0 / total) * 100.0) / 100.0);
            result.add(row);
        }
        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/analytics/teacher-workload
     * Scheduled periods per teacher.
     */
    @GetMapping("/teacher-workload")
    public ResponseEntity<List<Map<String, Object>>> teacherWorkload() {
        List<Teacher> teachers = teacherRepository.findAll();
        List<Map<String, Object>> result = new ArrayList<>();

        for (Teacher t : teachers) {
            List<Timetable> slots = timetableRepository.findByTeacher_TeacherIdAndIsActiveTrue(t.getTeacherId());
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("teacherId", t.getTeacherId());
            row.put("teacherName", t.getName());
            row.put("employeeId", t.getEmployeeId());
            row.put("scheduledPeriodsPerWeek", slots.size());
            row.put("subjectsAssigned", slots.stream()
                    .map(s -> s.getSubject().getSubjectName())
                    .distinct().collect(Collectors.toList()));
            result.add(row);
        }
        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/analytics/timetable/utilization
     * Shows how many of the available period×day slots are filled.
     */
    @GetMapping("/timetable/utilization")
    public ResponseEntity<Map<String, Object>> timetableUtilization() {
        List<Timetable> active = timetableRepository.findAll().stream()
                .filter(t -> Boolean.TRUE.equals(t.getIsActive()))
                .collect(Collectors.toList());

        long sectionCount = classSectionRepository.count();
        long maxSlots = sectionCount * 5 * 8; // 5 days × 8 periods per section
        long filledSlots = active.size();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("filledSlots", filledSlots);
        result.put("theoreticalMaxSlots", maxSlots);
        result.put("utilizationPercent", maxSlots == 0 ? 0 : Math.round((filledSlots * 100.0 / maxSlots) * 100.0) / 100.0);
        result.put("sectionsWithTimetable", active.stream()
                .map(t -> t.getSection().getSectionId()).distinct().count());
        result.put("totalSections", sectionCount);
        return ResponseEntity.ok(result);
    }
}
