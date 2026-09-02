package com.examly.springapp.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "holiday_calendar")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class HolidayCalendar {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long holidayId;

    @Column(nullable = false)
    private LocalDate holidayDate;

    private String description;

    @Enumerated(EnumType.STRING)
    private HolidayType holidayType;

    private String academicYear;
}
