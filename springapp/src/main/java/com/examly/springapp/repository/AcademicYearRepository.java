package com.examly.springapp.repository;

import com.examly.springapp.model.AcademicYear;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AcademicYearRepository extends JpaRepository<AcademicYear, Long> {
    Optional<AcademicYear> findByIsActiveTrue();
    boolean existsByYearLabel(String yearLabel);
}
