package com.examly.springapp.controller;

import com.examly.springapp.dto.SubjectTeacherMappingRequest;
import com.examly.springapp.model.*;
import com.examly.springapp.repository.*;
import com.examly.springapp.exception.DuplicateResourceException;
import com.examly.springapp.exception.ResourceNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/subject-teacher-mappings")
@RequiredArgsConstructor
public class SubjectTeacherMappingController {

    private final SubjectTeacherMappingRepository mappingRepository;
    private final ClassSectionRepository classSectionRepository;
    private final SubjectRepository subjectRepository;
    private final TeacherRepository teacherRepository;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','PRINCIPAL')")
    public ResponseEntity<SubjectTeacherMapping> create(@Valid @RequestBody SubjectTeacherMappingRequest request) {
        if (mappingRepository.existsBySection_SectionIdAndSubject_SubjectId(
                request.getSectionId(), request.getSubjectId())) {
            throw new DuplicateResourceException(
                    "A mapping for section " + request.getSectionId() + " and subject " + request.getSubjectId() + " already exists");
        }
        ClassSection section = classSectionRepository.findById(request.getSectionId())
                .orElseThrow(() -> new ResourceNotFoundException("Section not found: " + request.getSectionId()));
        Subject subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found: " + request.getSubjectId()));
        Teacher teacher = teacherRepository.findById(request.getTeacherId())
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found: " + request.getTeacherId()));

        SubjectTeacherMapping mapping = SubjectTeacherMapping.builder()
                .section(section)
                .subject(subject)
                .teacher(teacher)
                .periodsPerWeek(request.getPeriodsPerWeek())
                .requiresDoublePeriod(request.getRequiresDoublePeriod() != null && request.getRequiresDoublePeriod())
                .isActive(true)
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(mappingRepository.save(mapping));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<SubjectTeacherMapping>> getAll() {
        return ResponseEntity.ok(mappingRepository.findAll());
    }

    @GetMapping("/section/{sectionId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<SubjectTeacherMapping>> getBySection(@PathVariable Long sectionId) {
        return ResponseEntity.ok(mappingRepository.findBySection_SectionIdAndIsActiveTrue(sectionId));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','PRINCIPAL')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        SubjectTeacherMapping m = mappingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Mapping not found: " + id));
        m.setIsActive(false);
        mappingRepository.save(m);
        return ResponseEntity.noContent().build();
    }
}
