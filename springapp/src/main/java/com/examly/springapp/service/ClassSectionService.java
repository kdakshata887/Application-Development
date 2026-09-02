package com.examly.springapp.service;

import com.examly.springapp.model.ClassSection;

import java.util.List;

public interface ClassSectionService {
    ClassSection createSection(ClassSection section);
    List<ClassSection> getAllSections();
    ClassSection getSectionById(Long id);
    ClassSection updateSection(Long id, ClassSection section);
    void deleteSection(Long id);
}
