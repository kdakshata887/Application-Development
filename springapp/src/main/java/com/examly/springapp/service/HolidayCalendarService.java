package com.examly.springapp.service;

import com.examly.springapp.model.HolidayCalendar;

import java.util.List;

public interface HolidayCalendarService {
    HolidayCalendar createHoliday(HolidayCalendar holiday);
    List<HolidayCalendar> getAllHolidays();
    List<HolidayCalendar> getHolidaysByYear(String academicYear);
    HolidayCalendar updateHoliday(Long id, HolidayCalendar holiday);
    void deleteHoliday(Long id);
}
