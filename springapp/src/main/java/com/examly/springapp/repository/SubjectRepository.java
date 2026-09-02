package com.examly.springapp.repository;

import com.examly.springapp.model.Subject;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubjectRepository extends JpaRepository<Subject, Long> {
    boolean existsBySubjectCode(String subjectCode);
}
