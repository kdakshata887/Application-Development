package com.examly.springapp.controller;

import com.examly.springapp.dto.AcademicYearRequest;
import com.examly.springapp.exception.DuplicateResourceException;
import com.examly.springapp.exception.ResourceNotFoundException;
import com.examly.springapp.model.AcademicYear;
import com.examly.springapp.repository.AcademicYearRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/academic-years")
@RequiredArgsConstructor
public class AcademicYearController {

    private final AcademicYearRepository academicYearRepository;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','PRINCIPAL')")
    public ResponseEntity<AcademicYear> create(@Valid @RequestBody AcademicYearRequest request) {
        if (academicYearRepository.existsByYearLabel(request.getYearLabel())) {
            throw new DuplicateResourceException("Academic year already exists: " + request.getYearLabel());
        }
        // Deactivate current active year if setting new active
        if (Boolean.TRUE.equals(request.getIsActive())) {
            academicYearRepository.findByIsActiveTrue().ifPresent(y -> {
                y.setIsActive(false);
                academicYearRepository.save(y);
            });
        }
        AcademicYear year = AcademicYear.builder()
                .yearLabel(request.getYearLabel())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .isActive(Boolean.TRUE.equals(request.getIsActive()))
                .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(academicYearRepository.save(year));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<AcademicYear>> getAll() {
        return ResponseEntity.ok(academicYearRepository.findAll());
    }

    @GetMapping("/active")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AcademicYear> getActive() {
        return ResponseEntity.ok(
                academicYearRepository.findByIsActiveTrue()
                        .orElseThrow(() -> new ResourceNotFoundException("No active academic year configured")));
    }

    @PutMapping("/{id}/activate")
    @PreAuthorize("hasAnyRole('ADMIN','PRINCIPAL')")
    public ResponseEntity<AcademicYear> activate(@PathVariable Long id) {
        // Deactivate all others
        academicYearRepository.findByIsActiveTrue().ifPresent(y -> {
            y.setIsActive(false);
            academicYearRepository.save(y);
        });
        AcademicYear year = academicYearRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Academic year not found: " + id));
        year.setIsActive(true);
        return ResponseEntity.ok(academicYearRepository.save(year));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','PRINCIPAL')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!academicYearRepository.existsById(id)) {
            throw new ResourceNotFoundException("Academic year not found: " + id);
        }
        academicYearRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
