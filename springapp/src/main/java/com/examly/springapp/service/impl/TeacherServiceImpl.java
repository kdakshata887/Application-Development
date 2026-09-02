package com.examly.springapp.service.impl;

import com.examly.springapp.exception.DuplicateResourceException;
import com.examly.springapp.exception.ResourceNotFoundException;
import com.examly.springapp.model.Day;
import com.examly.springapp.model.Teacher;
import com.examly.springapp.repository.TeacherRepository;
import com.examly.springapp.repository.TimetableRepository;
import com.examly.springapp.service.TeacherService;
import com.examly.springapp.service.ValidationUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TeacherServiceImpl implements TeacherService {

    private final TeacherRepository teacherRepository;
    private final TimetableRepository timetableRepository;

    @Override
    public Teacher createTeacher(Teacher teacher) {
        ValidationUtil.validateName(teacher.getName());
        if (teacherRepository.existsByEmployeeId(teacher.getEmployeeId())) {
            throw new DuplicateResourceException("Employee id already exists: " + teacher.getEmployeeId());
        }
        return teacherRepository.save(teacher);
    }

    @Override
    public List<Teacher> getAllTeachers() {
        return teacherRepository.findAll();
    }

    @Override
    public Teacher getTeacherById(Long id) {
        return teacherRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found with id: " + id));
    }

    @Override
    public Teacher updateTeacher(Long id, Teacher update) {
        Teacher teacher = getTeacherById(id);
        if (update.getName() != null) {
            ValidationUtil.validateName(update.getName());
            teacher.setName(update.getName());
        }
        if (update.getQualification() != null) teacher.setQualification(update.getQualification());
        if (update.getSubjectSpecialization() != null) teacher.setSubjectSpecialization(update.getSubjectSpecialization());
        if (update.getBiometricDeviceRef() != null) teacher.setBiometricDeviceRef(update.getBiometricDeviceRef());
        if (update.getIsActive() != null) teacher.setIsActive(update.getIsActive());
        return teacherRepository.save(teacher);
    }

    @Override
    public void deleteTeacher(Long id) {
        Teacher teacher = getTeacherById(id);
        teacherRepository.delete(teacher);
    }

    @Override
    public List<Teacher> findAvailableSubstitutes(Long absentTeacherId, Day day, Integer period) {
        Set<Long> busyTeacherIds = timetableRepository.findAll().stream()
                .filter(t -> t.getDay() == day && t.getPeriod().equals(period) && Boolean.TRUE.equals(t.getIsActive()))
                .map(t -> t.getTeacher().getTeacherId())
                .collect(Collectors.toSet());

        return teacherRepository.findAll().stream()
                .filter(t -> Boolean.TRUE.equals(t.getIsActive()))
                .filter(t -> !t.getTeacherId().equals(absentTeacherId))
                .filter(t -> !busyTeacherIds.contains(t.getTeacherId()))
                .collect(Collectors.toList());
    }
}
