package com.examly.springapp.controller;

import com.examly.springapp.model.HolidayCalendar;
import com.examly.springapp.service.HolidayCalendarService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/holidays")
@RequiredArgsConstructor
public class HolidayCalendarController {

    private final HolidayCalendarService holidayCalendarService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','PRINCIPAL')")
    public ResponseEntity<HolidayCalendar> create(@RequestBody HolidayCalendar holiday) {
        return ResponseEntity.status(HttpStatus.CREATED).body(holidayCalendarService.createHoliday(holiday));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<HolidayCalendar>> getAll() {
        return ResponseEntity.ok(holidayCalendarService.getAllHolidays());
    }

    @GetMapping("/year/{academicYear}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<HolidayCalendar>> getByYear(@PathVariable String academicYear) {
        return ResponseEntity.ok(holidayCalendarService.getHolidaysByYear(academicYear));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','PRINCIPAL')")
    public ResponseEntity<HolidayCalendar> update(@PathVariable Long id, @RequestBody HolidayCalendar holiday) {
        return ResponseEntity.ok(holidayCalendarService.updateHoliday(id, holiday));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','PRINCIPAL')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        holidayCalendarService.deleteHoliday(id);
        return ResponseEntity.noContent().build();
    }
}
