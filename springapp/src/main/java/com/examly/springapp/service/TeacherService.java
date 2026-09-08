package com.examly.springapp.service;

import com.examly.springapp.dto.BulkDeleteResult;
import com.examly.springapp.model.Teacher;

import java.util.List;

public interface TeacherService {
    Teacher createTeacher(Teacher teacher);
    List<Teacher> getAllTeachers();
    Teacher getTeacherById(Long id);
    Teacher updateTeacher(Long id, Teacher teacher);
    void deleteTeacher(Long id);
    List<Teacher> findAvailableSubstitutes(Long absentTeacherId, com.examly.springapp.model.Day day, Integer period);
    BulkDeleteResult bulkDelete(List<Long> ids);
}
