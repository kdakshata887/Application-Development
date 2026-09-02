package com.examly.springapp.repository;

import com.examly.springapp.model.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TeacherRepository extends JpaRepository<Teacher, Long> {
    Optional<Teacher> findByEmployeeId(String employeeId);
    Optional<Teacher> findByUser_UserId(Long userId);
    boolean existsByEmployeeId(String employeeId);
}
