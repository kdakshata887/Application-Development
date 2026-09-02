package com.examly.springapp.repository;

import com.examly.springapp.model.HolidayCalendar;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface HolidayCalendarRepository extends JpaRepository<HolidayCalendar, Long> {
    List<HolidayCalendar> findByAcademicYear(String academicYear);
    Optional<HolidayCalendar> findByHolidayDate(LocalDate date);
}
