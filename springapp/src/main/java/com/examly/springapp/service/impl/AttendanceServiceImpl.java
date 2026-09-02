package com.examly.springapp.service.impl;

import com.examly.springapp.dto.AttendanceMarkRequest;
import com.examly.springapp.exception.AttendanceOverrideException;
import com.examly.springapp.exception.ResourceNotFoundException;
import com.examly.springapp.model.*;
import com.examly.springapp.repository.AttendanceRepository;
import com.examly.springapp.repository.StudentRepository;
import com.examly.springapp.repository.UserRepository;
import com.examly.springapp.service.AttendanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AttendanceServiceImpl implements AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;

    @Value("${app.attendance.low-threshold-percent}")
    private double lowThresholdPercent;

    @Override
    public Attendance markAttendance(AttendanceMarkRequest request, Long recordedByUserId) {
        Student student = studentRepository.findById(request.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + request.getStudentId()));

        User recordedBy = recordedByUserId != null
                ? userRepository.findById(recordedByUserId).orElse(null)
                : null;

        if (request.getCapturedBy() == CapturedBy.MANUAL &&
                (request.getModificationReason() == null || request.getModificationReason().isBlank())) {
            throw new AttendanceOverrideException("Reason is required for attendance modification");
        }

        Attendance attendance = attendanceRepository
                .findByStudent_StudentIdAndDate(request.getStudentId(), request.getDate())
                .orElse(Attendance.builder().student(student).date(request.getDate()).build());

        attendance.setStatus(request.getStatus());
        attendance.setCapturedBy(request.getCapturedBy() == null ? CapturedBy.MANUAL : request.getCapturedBy());
        attendance.setRecordedBy(recordedBy);
        attendance.setModificationReason(request.getModificationReason());
        attendance.setModifiedAt(LocalDateTime.now());

        return attendanceRepository.save(attendance);
    }

    @Override
    public Attendance overrideAttendance(Long attendanceId, AttendanceMarkRequest request, Long recordedByUserId) {
        if (request.getModificationReason() == null || request.getModificationReason().isBlank()) {
            throw new AttendanceOverrideException("Reason is required for attendance modification");
        }

        Attendance attendance = attendanceRepository.findById(attendanceId)
                .orElseThrow(() -> new ResourceNotFoundException("Attendance record not found with id: " + attendanceId));

        User recordedBy = recordedByUserId != null
                ? userRepository.findById(recordedByUserId).orElse(null)
                : null;

        attendance.setStatus(request.getStatus());
        attendance.setCapturedBy(CapturedBy.MANUAL);
        attendance.setRecordedBy(recordedBy);
        attendance.setModificationReason(request.getModificationReason());
        attendance.setModifiedAt(LocalDateTime.now());

        return attendanceRepository.save(attendance);
    }

    @Override
    public List<Attendance> getAttendanceByStudent(Long studentId) {
        return attendanceRepository.findByStudent_StudentId(studentId);
    }

    @Override
    public List<Attendance> getAttendanceByStudentAndRange(Long studentId, LocalDate from, LocalDate to) {
        return attendanceRepository.findByStudent_StudentIdAndDateBetween(studentId, from, to);
    }

    @Override
    public List<Attendance> getAttendanceBySectionAndDate(Long sectionId, LocalDate date) {
        return attendanceRepository.findByDateAndStudent_Section_SectionId(date, sectionId);
    }

    @Override
    public Map<String, Object> getAttendancePercentage(Long studentId) {
        long total = attendanceRepository.countByStudent_StudentId(studentId);
        long present = attendanceRepository.countByStudent_StudentIdAndStatus(studentId, AttendanceStatus.PRESENT)
                + attendanceRepository.countByStudent_StudentIdAndStatus(studentId, AttendanceStatus.LATE);

        double percentage = total == 0 ? 0.0 : (present * 100.0) / total;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("studentId", studentId);
        result.put("totalRecordedDays", total);
        result.put("presentDays", present);
        result.put("attendancePercentage", Math.round(percentage * 100.0) / 100.0);
        result.put("belowThreshold", percentage < lowThresholdPercent);
        return result;
    }

    @Override
    public List<Map<String, Object>> getBelow75List() {
        List<Student> students = studentRepository.findAll();
        return students.stream()
                .map(s -> getAttendancePercentage(s.getStudentId()))
                .filter(m -> Boolean.TRUE.equals(m.get("belowThreshold")))
                .collect(Collectors.toList());
    }
}
