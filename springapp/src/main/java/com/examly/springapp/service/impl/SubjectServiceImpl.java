package com.examly.springapp.service.impl;

import com.examly.springapp.dto.BulkDeleteResult;
import com.examly.springapp.exception.DuplicateResourceException;
import com.examly.springapp.exception.ResourceNotFoundException;
import com.examly.springapp.model.Subject;
import com.examly.springapp.repository.SubjectRepository;
import com.examly.springapp.repository.TimetableRepository;
import com.examly.springapp.service.SubjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SubjectServiceImpl implements SubjectService {

    private final SubjectRepository subjectRepository;
    private final TimetableRepository timetableRepository;

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

    /**
     * Bulk-delete subjects by ID.
     * Subjects used in active timetable entries cannot be deleted.
     */
    @Override
    @Transactional
    public BulkDeleteResult bulkDelete(List<Long> ids) {
        List<Long> deletedIds = new ArrayList<>();
        List<BulkDeleteResult.FailedEntry> failed = new ArrayList<>();

        for (Long id : ids) {
            try {
                Subject subject = subjectRepository.findById(id).orElse(null);
                if (subject == null) {
                    failed.add(new BulkDeleteResult.FailedEntry(id, "Subject not found"));
                    continue;
                }
                if (timetableRepository.existsBySubject_SubjectIdAndIsActiveTrue(id)) {
                    failed.add(new BulkDeleteResult.FailedEntry(id,
                            "Cannot delete subject '" + subject.getSubjectName() + "' — used in active timetable entries. Remove from timetable first."));
                    continue;
                }
                subjectRepository.delete(subject);
                deletedIds.add(id);
            } catch (Exception e) {
                failed.add(new BulkDeleteResult.FailedEntry(id, "Deletion failed: " + e.getMessage()));
            }
        }

        return BulkDeleteResult.builder()
                .deletedCount(deletedIds.size())
                .failedCount(failed.size())
                .deletedIds(deletedIds)
                .failed(failed)
                .build();
    }
}
