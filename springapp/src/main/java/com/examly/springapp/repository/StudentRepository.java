package com.examly.springapp.repository;

import com.examly.springapp.model.Student;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Long> {
    Optional<Student> findByAdmissionNumber(String admissionNumber);
    Optional<Student> findByUser_UserId(Long userId);
    List<Student> findBySection_SectionId(Long sectionId);
    List<Student> findByParent_UserId(Long parentUserId);
    boolean existsByAdmissionNumber(String admissionNumber);
}
