package com.examly.springapp.service.impl;

import com.examly.springapp.dto.BulkDeleteResult;
import com.examly.springapp.exception.DuplicateResourceException;
import com.examly.springapp.exception.ResourceNotFoundException;
import com.examly.springapp.model.Student;
import com.examly.springapp.repository.AttendanceRepository;
import com.examly.springapp.repository.StudentRepository;
import com.examly.springapp.service.StudentService;
import com.examly.springapp.service.ValidationUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StudentServiceImpl implements StudentService {

    private final StudentRepository studentRepository;
    private final AttendanceRepository attendanceRepository;

    @Override
    public Student createStudent(Student student) {
        ValidationUtil.validateName(student.getName());
        if (studentRepository.existsByAdmissionNumber(student.getAdmissionNumber())) {
            throw new DuplicateResourceException("Admission number already exists: " + student.getAdmissionNumber());
        }
        return studentRepository.save(student);
    }

    @Override
    public List<Student> getAllStudents() {
        return studentRepository.findAll();
    }

    @Override
    public Student getStudentById(Long id) {
        return studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + id));
    }

    @Override
    public List<Student> getStudentsBySection(Long sectionId) {
        return studentRepository.findBySection_SectionId(sectionId);
    }

    @Override
    public List<Student> getStudentsByParent(Long parentUserId) {
        return studentRepository.findByParent_UserId(parentUserId);
    }

    @Override
    public Student updateStudent(Long id, Student update) {
        Student student = getStudentById(id);
        if (update.getName() != null) {
            ValidationUtil.validateName(update.getName());
            student.setName(update.getName());
        }
        if (update.getSection() != null) student.setSection(update.getSection());
        if (update.getParent() != null) student.setParent(update.getParent());
        if (update.getBiometricDeviceRef() != null) student.setBiometricDeviceRef(update.getBiometricDeviceRef());
        if (update.getDateOfBirth() != null) student.setDateOfBirth(update.getDateOfBirth());
        if (update.getIsActive() != null) student.setIsActive(update.getIsActive());
        return studentRepository.save(student);
    }

    @Override
    public void deleteStudent(Long id) {
        Student student = getStudentById(id);
        studentRepository.delete(student);
    }

    /**
     * Bulk-delete students by ID.
     * Strategy: Students with attendance records are blocked (historical data preserved).
     * Students without attendance records are hard-deleted along with their linked User account.
     */
    @Override
    @Transactional
    public BulkDeleteResult bulkDelete(List<Long> ids) {
        List<Long> deletedIds = new ArrayList<>();
        List<BulkDeleteResult.FailedEntry> failed = new ArrayList<>();

        for (Long id : ids) {
            try {
                Student student = studentRepository.findById(id).orElse(null);
                if (student == null) {
                    failed.add(new BulkDeleteResult.FailedEntry(id, "Student not found"));
                    continue;
                }
                long attendanceCount = attendanceRepository.countByStudent_StudentId(id);
                if (attendanceCount > 0) {
                    failed.add(new BulkDeleteResult.FailedEntry(id,
                            "Cannot delete '" + student.getName() + "' — has " + attendanceCount + " attendance record(s). Deactivate instead."));
                    continue;
                }
                studentRepository.delete(student);
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
