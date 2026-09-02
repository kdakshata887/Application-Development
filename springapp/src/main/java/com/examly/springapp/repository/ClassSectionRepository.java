package com.examly.springapp.repository;

import com.examly.springapp.model.ClassSection;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClassSectionRepository extends JpaRepository<ClassSection, Long> {
}
