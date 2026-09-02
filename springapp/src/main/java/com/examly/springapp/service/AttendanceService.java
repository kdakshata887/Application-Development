package com.examly.springapp.service;

import com.examly.springapp.dto.AttendanceMarkRequest;
import com.examly.springapp.model.Attendance;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface AttendanceService {
    Attendance markAttendance(AttendanceMarkRequest request, Long recordedByUserId);
    Attendance overrideAttendance(Long attendanceId, AttendanceMarkRequest request, Long recordedByUserId);
    List<Attendance> getAttendanceByStudent(Long studentId);
    List<Attendance> getAttendanceByStudentAndRange(Long studentId, LocalDate from, LocalDate to);
    List<Attendance> getAttendanceBySectionAndDate(Long sectionId, LocalDate date);
    Map<String, Object> getAttendancePercentage(Long studentId);
    List<Map<String, Object>> getBelow75List();
}
