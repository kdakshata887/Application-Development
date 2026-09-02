package com.examly.springapp.repository;

import com.examly.springapp.model.LeaveApplication;
import com.examly.springapp.model.LeaveStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface LeaveApplicationRepository extends JpaRepository<LeaveApplication, Long> {
    List<LeaveApplication> findByTeacher_TeacherId(Long teacherId);
    List<LeaveApplication> findByStatus(LeaveStatus status);

    /**
     * Find teachers who have overlapping active/pending leaves in a date range.
     */
    @Query("SELECT l.teacher.teacherId FROM LeaveApplication l " +
           "WHERE l.status IN ('PENDING','APPROVED') " +
           "AND l.fromDate <= :toDate AND l.toDate >= :fromDate")
    List<Long> findTeacherIdsWithLeavesOverlapping(
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);
}

