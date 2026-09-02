package com.examly.springapp.service;

import com.examly.springapp.dto.AttendanceMarkRequest;
import com.examly.springapp.exception.ResourceNotFoundException;
import com.examly.springapp.model.*;
import com.examly.springapp.repository.*;
import com.examly.springapp.service.impl.AttendanceServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AttendanceServiceImpl.
 */
@ExtendWith(MockitoExtension.class)
class AttendanceServiceTest {

    @Mock AttendanceRepository attendanceRepository;
    @Mock StudentRepository studentRepository;
    @Mock UserRepository userRepository;

    @InjectMocks
    AttendanceServiceImpl attendanceService;

    @Test
    void markAttendance_shouldCreateRecord_whenStudentExists() {
        Student student = Student.builder().studentId(1L).name("Alice").isActive(true).build();
        User teacher = User.builder().userId(10L).username("teacher1").role(Role.TEACHER).build();

        AttendanceMarkRequest req = AttendanceMarkRequest.builder()
                .studentId(1L).date(LocalDate.now()).status(AttendanceStatus.PRESENT)
                .capturedBy(CapturedBy.BIOMETRIC).build();

        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(userRepository.findById(10L)).thenReturn(Optional.of(teacher));
        when(attendanceRepository.findByStudent_StudentIdAndDate(1L, LocalDate.now())).thenReturn(Optional.empty());
        Attendance saved = Attendance.builder().attendanceId(1L).student(student).status(AttendanceStatus.PRESENT)
                .date(LocalDate.now()).capturedBy(CapturedBy.BIOMETRIC).recordedBy(teacher).build();
        when(attendanceRepository.save(any())).thenReturn(saved);

        Attendance result = attendanceService.markAttendance(req, 10L);

        assertThat(result.getStatus()).isEqualTo(AttendanceStatus.PRESENT);
        assertThat(result.getStudent().getStudentId()).isEqualTo(1L);
    }

    @Test
    void markAttendance_shouldThrow_whenStudentNotFound() {
        AttendanceMarkRequest req = AttendanceMarkRequest.builder()
                .studentId(999L).date(LocalDate.now()).status(AttendanceStatus.PRESENT)
                .capturedBy(CapturedBy.BIOMETRIC).build();

        when(studentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> attendanceService.markAttendance(req, 1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Student");
    }

    @Test
    void getAttendancePercentage_shouldReturn100_whenAllDaysPresent() {
        when(attendanceRepository.countByStudent_StudentId(1L)).thenReturn(10L);
        when(attendanceRepository.countByStudent_StudentIdAndStatus(1L, AttendanceStatus.PRESENT)).thenReturn(10L);
        when(attendanceRepository.countByStudent_StudentIdAndStatus(1L, AttendanceStatus.LATE)).thenReturn(0L);

        Map<String, Object> result = attendanceService.getAttendancePercentage(1L);

        assertThat((Double) result.get("attendancePercentage")).isEqualTo(100.0);
    }

    @Test
    void getAttendancePercentage_shouldReturn0_whenNoRecords() {
        when(attendanceRepository.countByStudent_StudentId(1L)).thenReturn(0L);
        when(attendanceRepository.countByStudent_StudentIdAndStatus(eq(1L), any())).thenReturn(0L);

        Map<String, Object> result = attendanceService.getAttendancePercentage(1L);

        assertThat((Double) result.get("attendancePercentage")).isEqualTo(0.0);
    }

    @Test
    void getBelow75List_shouldOnlyReturnStudentsBelowThreshold() {
        ReflectionTestUtils.setField(attendanceService, "lowThresholdPercent", 75.0);

        Student s1 = Student.builder().studentId(1L).name("Alice").admissionNumber("ADM001").build();
        Student s2 = Student.builder().studentId(2L).name("Bob").admissionNumber("ADM002").build();

        when(studentRepository.findAll()).thenReturn(List.of(s1, s2));

        // Alice: 70% attendance (7/10)
        when(attendanceRepository.countByStudent_StudentId(1L)).thenReturn(10L);
        when(attendanceRepository.countByStudent_StudentIdAndStatus(1L, AttendanceStatus.PRESENT)).thenReturn(7L);
        when(attendanceRepository.countByStudent_StudentIdAndStatus(1L, AttendanceStatus.LATE)).thenReturn(0L);

        // Bob: 90% attendance (9/10)
        when(attendanceRepository.countByStudent_StudentId(2L)).thenReturn(10L);
        when(attendanceRepository.countByStudent_StudentIdAndStatus(2L, AttendanceStatus.PRESENT)).thenReturn(9L);
        when(attendanceRepository.countByStudent_StudentIdAndStatus(2L, AttendanceStatus.LATE)).thenReturn(0L);

        List<Map<String, Object>> result = attendanceService.getBelow75List();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).get("studentId")).isEqualTo(1L);
        assertThat((Double) result.get(0).get("attendancePercentage")).isLessThan(75.0);
    }
}
