package com.examly.springapp.controller;

import com.examly.springapp.model.ClassSection;
import com.examly.springapp.service.ClassSectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sections")
@RequiredArgsConstructor
public class ClassSectionController {

    private final ClassSectionService classSectionService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','PRINCIPAL')")
    public ResponseEntity<ClassSection> createSection(@RequestBody ClassSection section) {
        return ResponseEntity.status(HttpStatus.CREATED).body(classSectionService.createSection(section));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ClassSection>> getAllSections() {
        return ResponseEntity.ok(classSectionService.getAllSections());
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ClassSection> getSectionById(@PathVariable Long id) {
        return ResponseEntity.ok(classSectionService.getSectionById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','PRINCIPAL')")
    public ResponseEntity<ClassSection> updateSection(@PathVariable Long id, @RequestBody ClassSection section) {
        return ResponseEntity.ok(classSectionService.updateSection(id, section));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','PRINCIPAL')")
    public ResponseEntity<Void> deleteSection(@PathVariable Long id) {
        classSectionService.deleteSection(id);
        return ResponseEntity.noContent().build();
    }
}
