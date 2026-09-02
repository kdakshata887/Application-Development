package com.examly.springapp.repository;

import com.examly.springapp.model.Attendance;
import com.examly.springapp.model.AttendanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    List<Attendance> findByStudent_StudentId(Long studentId);
    List<Attendance> findByStudent_StudentIdAndDateBetween(Long studentId, LocalDate from, LocalDate to);
    List<Attendance> findByDateAndStudent_Section_SectionId(LocalDate date, Long sectionId);
    Optional<Attendance> findByStudent_StudentIdAndDate(Long studentId, LocalDate date);
    long countByStudent_StudentIdAndStatus(Long studentId, AttendanceStatus status);
    long countByStudent_StudentId(Long studentId);
    List<Attendance> findByDate(LocalDate date);
}
