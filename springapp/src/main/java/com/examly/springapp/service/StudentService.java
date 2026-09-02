package com.examly.springapp.service;

import com.examly.springapp.model.Student;

import java.util.List;

public interface StudentService {
    Student createStudent(Student student);
    List<Student> getAllStudents();
    Student getStudentById(Long id);
    List<Student> getStudentsBySection(Long sectionId);
    List<Student> getStudentsByParent(Long parentUserId);
    Student updateStudent(Long id, Student student);
    void deleteStudent(Long id);
}
