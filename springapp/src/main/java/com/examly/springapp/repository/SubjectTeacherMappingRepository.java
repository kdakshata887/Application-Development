package com.examly.springapp.repository;

import com.examly.springapp.model.SubjectTeacherMapping;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubjectTeacherMappingRepository extends JpaRepository<SubjectTeacherMapping, Long> {
    List<SubjectTeacherMapping> findBySection_SectionIdAndIsActiveTrue(Long sectionId);
    List<SubjectTeacherMapping> findByTeacher_TeacherIdAndIsActiveTrue(Long teacherId);
    boolean existsBySection_SectionIdAndSubject_SubjectId(Long sectionId, Long subjectId);
}
