package com.examly.springapp.service.impl;

import com.examly.springapp.dto.BulkDeleteResult;
import com.examly.springapp.exception.ResourceNotFoundException;
import com.examly.springapp.model.ClassSection;
import com.examly.springapp.repository.ClassSectionRepository;
import com.examly.springapp.repository.StudentRepository;
import com.examly.springapp.repository.SubjectTeacherMappingRepository;
import com.examly.springapp.repository.TimetableRepository;
import com.examly.springapp.service.ClassSectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ClassSectionServiceImpl implements ClassSectionService {

    private final ClassSectionRepository classSectionRepository;
    private final StudentRepository studentRepository;
    private final TimetableRepository timetableRepository;
    private final SubjectTeacherMappingRepository mappingRepository;

    @Override
    public ClassSection createSection(ClassSection section) {
        return classSectionRepository.save(section);
    }

    @Override
    public List<ClassSection> getAllSections() {
        return classSectionRepository.findAll();
    }

    @Override
    public ClassSection getSectionById(Long id) {
        return classSectionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Class section not found with id: " + id));
    }

    @Override
    public ClassSection updateSection(Long id, ClassSection update) {
        ClassSection section = getSectionById(id);
        if (update.getClassName() != null) section.setClassName(update.getClassName());
        if (update.getSectionName() != null) section.setSectionName(update.getSectionName());
        if (update.getClassTeacher() != null) section.setClassTeacher(update.getClassTeacher());
        return classSectionRepository.save(section);
    }

    @Override
    public void deleteSection(Long id) {
        classSectionRepository.delete(getSectionById(id));
    }

    /**
     * Bulk-delete class sections by ID.
     * A section with enrolled students OR active timetable entries cannot be deleted.
     */
    @Override
    @Transactional
    public BulkDeleteResult bulkDelete(List<Long> ids) {
        List<Long> deletedIds = new ArrayList<>();
        List<BulkDeleteResult.FailedEntry> failed = new ArrayList<>();

        for (Long id : ids) {
            try {
                ClassSection section = classSectionRepository.findById(id).orElse(null);
                if (section == null) {
                    failed.add(new BulkDeleteResult.FailedEntry(id, "Section not found"));
                    continue;
                }
                long studentCount = studentRepository.countBySection_SectionId(id);
                if (studentCount > 0) {
                    failed.add(new BulkDeleteResult.FailedEntry(id,
                            "Cannot delete section '" + section.getSectionName() + "' — has " + studentCount + " enrolled student(s). Reassign students first."));
                    continue;
                }
                if (timetableRepository.existsBySection_SectionIdAndIsActiveTrue(id)) {
                    failed.add(new BulkDeleteResult.FailedEntry(id,
                            "Cannot delete section '" + section.getSectionName() + "' — has active timetable entries. Remove timetable first."));
                    continue;
                }
                // Remove inactive mappings before deleting
                mappingRepository.findBySection_SectionIdAndIsActiveTrue(id)
                        .forEach(m -> m.setIsActive(false));
                classSectionRepository.delete(section);
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
