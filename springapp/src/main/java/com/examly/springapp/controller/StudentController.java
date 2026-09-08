package com.examly.springapp.controller;

import com.examly.springapp.dto.BulkDeleteRequest;
import com.examly.springapp.dto.BulkDeleteResult;
import com.examly.springapp.model.Student;
import com.examly.springapp.service.StudentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/students")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','PRINCIPAL')")
    public ResponseEntity<Student> createStudent(@Valid @RequestBody Student student) {
        return ResponseEntity.status(HttpStatus.CREATED).body(studentService.createStudent(student));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','PRINCIPAL','TEACHER','CLASS_TEACHER')")
    public ResponseEntity<List<Student>> getAllStudents() {
        return ResponseEntity.ok(studentService.getAllStudents());
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Student> getStudentById(@PathVariable Long id) {
        return ResponseEntity.ok(studentService.getStudentById(id));
    }

    @GetMapping("/section/{sectionId}")
    @PreAuthorize("hasAnyRole('ADMIN','PRINCIPAL','TEACHER','CLASS_TEACHER')")
    public ResponseEntity<List<Student>> getBySection(@PathVariable Long sectionId) {
        return ResponseEntity.ok(studentService.getStudentsBySection(sectionId));
    }

    @GetMapping("/parent/{parentUserId}")
    @PreAuthorize("hasAnyRole('ADMIN','PRINCIPAL') or #parentUserId == authentication.principal.userId")
    public ResponseEntity<List<Student>> getByParent(@PathVariable Long parentUserId) {
        return ResponseEntity.ok(studentService.getStudentsByParent(parentUserId));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','PRINCIPAL','CLASS_TEACHER')")
    public ResponseEntity<Student> updateStudent(@PathVariable Long id, @RequestBody Student student) {
        return ResponseEntity.ok(studentService.updateStudent(id, student));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','PRINCIPAL')")
    public ResponseEntity<Void> deleteStudent(@PathVariable Long id) {
        studentService.deleteStudent(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * POST /api/students/bulk-delete
     * Bulk-delete students. Only ADMIN/PRINCIPAL can call this.
     * Returns a detailed result for partial failures.
     */
    @PostMapping("/bulk-delete")
    @PreAuthorize("hasAnyRole('ADMIN','PRINCIPAL')")
    public ResponseEntity<BulkDeleteResult> bulkDelete(@Valid @RequestBody BulkDeleteRequest request) {
        BulkDeleteResult result = studentService.bulkDelete(request.getIds());
        return ResponseEntity.ok(result);
    }
}
