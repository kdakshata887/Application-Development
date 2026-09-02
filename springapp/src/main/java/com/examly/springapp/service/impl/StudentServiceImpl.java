package com.examly.springapp.service.impl;

import com.examly.springapp.exception.DuplicateResourceException;
import com.examly.springapp.exception.ResourceNotFoundException;
import com.examly.springapp.model.Student;
import com.examly.springapp.repository.StudentRepository;
import com.examly.springapp.service.StudentService;
import com.examly.springapp.service.ValidationUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StudentServiceImpl implements StudentService {

    private final StudentRepository studentRepository;

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
}
