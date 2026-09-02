package com.examly.springapp.repository;

import com.examly.springapp.model.Day;
import com.examly.springapp.model.Timetable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TimetableRepository extends JpaRepository<Timetable, Long> {
    List<Timetable> findBySection_SectionIdAndIsActiveTrue(Long sectionId);
    List<Timetable> findByTeacher_TeacherIdAndIsActiveTrue(Long teacherId);
    List<Timetable> findByTeacher_TeacherIdAndDayAndPeriodAndIsActiveTrue(Long teacherId, Day day, Integer period);
    List<Timetable> findByRoom_RoomIdAndDayAndPeriodAndIsActiveTrue(Long roomId, Day day, Integer period);
    List<Timetable> findBySection_SectionIdAndDayAndPeriodAndIsActiveTrue(Long sectionId, Day day, Integer period);
    List<Timetable> findByTeacher_TeacherIdAndDayAndIsActiveTrue(Long teacherId, Day day);
}
