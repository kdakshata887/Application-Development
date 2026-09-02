package com.examly.springapp.service.impl;

import com.examly.springapp.exception.ResourceNotFoundException;
import com.examly.springapp.model.HolidayCalendar;
import com.examly.springapp.repository.HolidayCalendarRepository;
import com.examly.springapp.service.HolidayCalendarService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class HolidayCalendarServiceImpl implements HolidayCalendarService {

    private final HolidayCalendarRepository holidayCalendarRepository;

    @Override
    public HolidayCalendar createHoliday(HolidayCalendar holiday) {
        return holidayCalendarRepository.save(holiday);
    }

    @Override
    public List<HolidayCalendar> getAllHolidays() {
        return holidayCalendarRepository.findAll();
    }

    @Override
    public List<HolidayCalendar> getHolidaysByYear(String academicYear) {
        return holidayCalendarRepository.findByAcademicYear(academicYear);
    }

    @Override
    public HolidayCalendar updateHoliday(Long id, HolidayCalendar update) {
        HolidayCalendar holiday = holidayCalendarRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Holiday not found with id: " + id));
        if (update.getHolidayDate() != null) holiday.setHolidayDate(update.getHolidayDate());
        if (update.getDescription() != null) holiday.setDescription(update.getDescription());
        if (update.getHolidayType() != null) holiday.setHolidayType(update.getHolidayType());
        if (update.getAcademicYear() != null) holiday.setAcademicYear(update.getAcademicYear());
        return holidayCalendarRepository.save(holiday);
    }

    @Override
    public void deleteHoliday(Long id) {
        HolidayCalendar holiday = holidayCalendarRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Holiday not found with id: " + id));
        holidayCalendarRepository.delete(holiday);
    }
}
