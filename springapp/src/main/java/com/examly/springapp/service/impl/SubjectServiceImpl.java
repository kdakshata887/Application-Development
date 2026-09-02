package com.examly.springapp.service.impl;

import com.examly.springapp.exception.DuplicateResourceException;
import com.examly.springapp.exception.ResourceNotFoundException;
import com.examly.springapp.model.Subject;
import com.examly.springapp.repository.SubjectRepository;
import com.examly.springapp.service.SubjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SubjectServiceImpl implements SubjectService {

    private final SubjectRepository subjectRepository;

    @Override
    public Subject createSubject(Subject subject) {
        if (subjectRepository.existsBySubjectCode(subject.getSubjectCode())) {
            throw new DuplicateResourceException("Subject code already exists: " + subject.getSubjectCode());
        }
        return subjectRepository.save(subject);
    }

    @Override
    public List<Subject> getAllSubjects() {
        return subjectRepository.findAll();
    }

    @Override
    public Subject getSubjectById(Long id) {
        return subjectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found with id: " + id));
    }

    @Override
    public Subject updateSubject(Long id, Subject update) {
        Subject subject = getSubjectById(id);
        if (update.getSubjectName() != null) subject.setSubjectName(update.getSubjectName());
        if (update.getPeriodsPerWeek() != null) subject.setPeriodsPerWeek(update.getPeriodsPerWeek());
        return subjectRepository.save(subject);
    }

    @Override
    public void deleteSubject(Long id) {
        subjectRepository.delete(getSubjectById(id));
    }
}
