package com.examly.springapp.service;

import com.examly.springapp.dto.TimetableRequest;
import com.examly.springapp.model.Timetable;

import java.util.List;

public interface TimetableService {
    Timetable createTimetableEntry(TimetableRequest request);
    Timetable updateTimetableEntry(Long id, TimetableRequest request);
    List<Timetable> getAllTimetables();
    Timetable getTimetableById(Long id);
    List<Timetable> getTimetableBySection(Long sectionId);
    List<Timetable> getTimetableByTeacher(Long teacherId);
    void deleteTimetableEntry(Long id);
}
