package com.examly.springapp.repository;

import com.examly.springapp.model.LeaveApplication;
import com.examly.springapp.model.LeaveStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LeaveApplicationRepository extends JpaRepository<LeaveApplication, Long> {
    List<LeaveApplication> findByTeacher_TeacherId(Long teacherId);
    List<LeaveApplication> findByStatus(LeaveStatus status);
}
