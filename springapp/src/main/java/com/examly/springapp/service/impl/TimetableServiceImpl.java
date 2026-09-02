package com.examly.springapp.service.impl;

import com.examly.springapp.dto.TimetableRequest;
import com.examly.springapp.exception.ResourceNotFoundException;
import com.examly.springapp.exception.TimetableClashException;
import com.examly.springapp.model.*;
import com.examly.springapp.repository.*;
import com.examly.springapp.service.TimetableService;
import com.examly.springapp.service.ValidationUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TimetableServiceImpl implements TimetableService {

    private final TimetableRepository timetableRepository;
    private final ClassSectionRepository classSectionRepository;
    private final SubjectRepository subjectRepository;
    private final TeacherRepository teacherRepository;
    private final RoomRepository roomRepository;

    private void validateNoClash(Long teacherId, Long roomId, Long sectionId, Day day, Integer period, Long excludeId) {
        List<Timetable> teacherClashes = timetableRepository
                .findByTeacher_TeacherIdAndDayAndPeriodAndIsActiveTrue(teacherId, day, period);
        teacherClashes.removeIf(t -> t.getTimetableId().equals(excludeId));
        if (!teacherClashes.isEmpty()) {
            throw new TimetableClashException("Teacher already assigned at this time slot — cannot double-book");
        }

        List<Timetable> sectionClashes = timetableRepository
                .findBySection_SectionIdAndDayAndPeriodAndIsActiveTrue(sectionId, day, period);
        sectionClashes.removeIf(t -> t.getTimetableId().equals(excludeId));
        if (!sectionClashes.isEmpty()) {
            throw new TimetableClashException("This class section already has a subject scheduled at this time slot");
        }

        if (roomId != null) {
            List<Timetable> roomClashes = timetableRepository
                    .findByRoom_RoomIdAndDayAndPeriodAndIsActiveTrue(roomId, day, period);
            roomClashes.removeIf(t -> t.getTimetableId().equals(excludeId));
            if (!roomClashes.isEmpty()) {
                throw new TimetableClashException("Room already booked at this time slot");
            }
        }
    }

    @Override
    public Timetable createTimetableEntry(TimetableRequest request) {
        ValidationUtil.validatePeriod(request.getPeriod());

        ClassSection section = classSectionRepository.findById(request.getSectionId())
                .orElseThrow(() -> new ResourceNotFoundException("Class section not found with id: " + request.getSectionId()));
        Subject subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found with id: " + request.getSubjectId()));
        Teacher teacher = teacherRepository.findById(request.getTeacherId())
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found with id: " + request.getTeacherId()));
        Room room = null;
        if (request.getRoomId() != null) {
            room = roomRepository.findById(request.getRoomId())
                    .orElseThrow(() -> new ResourceNotFoundException("Room not found with id: " + request.getRoomId()));
        }

        validateNoClash(teacher.getTeacherId(), request.getRoomId(), section.getSectionId(), request.getDay(), request.getPeriod(), null);

        Timetable timetable = Timetable.builder()
                .section(section)
                .day(request.getDay())
                .period(request.getPeriod())
                .subject(subject)
                .teacher(teacher)
                .room(room)
                .effectiveFrom(request.getEffectiveFrom())
                .isActive(true)
                .version(1)
                .build();

        return timetableRepository.save(timetable);
    }

    @Override
    public Timetable updateTimetableEntry(Long id, TimetableRequest request) {
        ValidationUtil.validatePeriod(request.getPeriod());

        Timetable existing = getTimetableById(id);

        ClassSection section = classSectionRepository.findById(request.getSectionId())
                .orElseThrow(() -> new ResourceNotFoundException("Class section not found with id: " + request.getSectionId()));
        Subject subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found with id: " + request.getSubjectId()));
        Teacher teacher = teacherRepository.findById(request.getTeacherId())
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found with id: " + request.getTeacherId()));
        Room room = null;
        if (request.getRoomId() != null) {
            room = roomRepository.findById(request.getRoomId())
                    .orElseThrow(() -> new ResourceNotFoundException("Room not found with id: " + request.getRoomId()));
        }

        validateNoClash(teacher.getTeacherId(), request.getRoomId(), section.getSectionId(), request.getDay(), request.getPeriod(), id);

        existing.setSection(section);
        existing.setDay(request.getDay());
        existing.setPeriod(request.getPeriod());
        existing.setSubject(subject);
        existing.setTeacher(teacher);
        existing.setRoom(room);
        existing.setEffectiveFrom(request.getEffectiveFrom());
        existing.setVersion(existing.getVersion() == null ? 2 : existing.getVersion() + 1);

        return timetableRepository.save(existing);
    }

    @Override
    public List<Timetable> getAllTimetables() {
        return timetableRepository.findAll();
    }

    @Override
    public Timetable getTimetableById(Long id) {
        return timetableRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Timetable entry not found with id: " + id));
    }

    @Override
    public List<Timetable> getTimetableBySection(Long sectionId) {
        return timetableRepository.findBySection_SectionIdAndIsActiveTrue(sectionId);
    }

    @Override
    public List<Timetable> getTimetableByTeacher(Long teacherId) {
        return timetableRepository.findByTeacher_TeacherIdAndIsActiveTrue(teacherId);
    }

    @Override
    public void deleteTimetableEntry(Long id) {
        Timetable timetable = getTimetableById(id);
        timetable.setIsActive(false);
        timetableRepository.save(timetable);
    }
}
