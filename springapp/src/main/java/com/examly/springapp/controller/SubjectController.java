package com.examly.springapp.controller;

import com.examly.springapp.dto.BulkDeleteRequest;
import com.examly.springapp.dto.BulkDeleteResult;
import com.examly.springapp.model.Subject;
import com.examly.springapp.service.SubjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/subjects")
@RequiredArgsConstructor
public class SubjectController {

    private final SubjectService subjectService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','PRINCIPAL')")
    public ResponseEntity<Subject> createSubject(@RequestBody Subject subject) {
        return ResponseEntity.status(HttpStatus.CREATED).body(subjectService.createSubject(subject));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Subject>> getAllSubjects() {
        return ResponseEntity.ok(subjectService.getAllSubjects());
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Subject> getSubjectById(@PathVariable Long id) {
        return ResponseEntity.ok(subjectService.getSubjectById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','PRINCIPAL')")
    public ResponseEntity<Subject> updateSubject(@PathVariable Long id, @RequestBody Subject subject) {
        return ResponseEntity.ok(subjectService.updateSubject(id, subject));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','PRINCIPAL')")
    public ResponseEntity<Void> deleteSubject(@PathVariable Long id) {
        subjectService.deleteSubject(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/bulk-delete")
    @PreAuthorize("hasAnyRole('ADMIN','PRINCIPAL')")
    public ResponseEntity<BulkDeleteResult> bulkDelete(@Valid @RequestBody BulkDeleteRequest request) {
        return ResponseEntity.ok(subjectService.bulkDelete(request.getIds()));
    }
}
