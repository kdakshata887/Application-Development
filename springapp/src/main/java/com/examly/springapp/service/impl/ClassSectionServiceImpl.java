package com.examly.springapp.service.impl;

import com.examly.springapp.exception.ResourceNotFoundException;
import com.examly.springapp.model.ClassSection;
import com.examly.springapp.repository.ClassSectionRepository;
import com.examly.springapp.service.ClassSectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ClassSectionServiceImpl implements ClassSectionService {

    private final ClassSectionRepository classSectionRepository;

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
}
