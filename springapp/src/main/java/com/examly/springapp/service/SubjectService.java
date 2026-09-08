package com.examly.springapp.service;

import com.examly.springapp.dto.BulkDeleteResult;
import com.examly.springapp.model.Subject;

import java.util.List;

public interface SubjectService {
    Subject createSubject(Subject subject);
    List<Subject> getAllSubjects();
    Subject getSubjectById(Long id);
    Subject updateSubject(Long id, Subject subject);
    void deleteSubject(Long id);
    BulkDeleteResult bulkDelete(List<Long> ids);
}
