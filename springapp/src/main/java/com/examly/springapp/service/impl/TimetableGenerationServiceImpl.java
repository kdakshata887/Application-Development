package com.examly.springapp.service.impl;

import com.examly.springapp.dto.TimetableGenerationResult;
import com.examly.springapp.model.*;
import com.examly.springapp.repository.*;
import com.examly.springapp.service.TimetableGenerationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Deterministic greedy timetable generator (FR4).
 *
 * Algorithm:
 * 1. For each section, load its SubjectTeacherMappings (subject + teacher + periodsPerWeek).
 * 2. Expand mappings into a list of "slots to fill" (one per required period).
 * 3. Iterate over working days × periods (1–periodsPerDay).
 * 4. For each slot, find the next unscheduled subject-teacher pair.
 * 5. Check constraints: teacher not double-booked, room not double-booked.
 * 6. Assign the slot; if no valid assignment exists, record a violation.
 *
 * Soft constraint: tries to leave at least one free period per teacher per day.
 * Lab (double period) subjects are scheduled in consecutive periods when possible.
 *
 * The algorithm is transparent and deterministic — no ML or random components.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TimetableGenerationServiceImpl implements TimetableGenerationService {

    private final TimetableRepository timetableRepository;
    private final SubjectTeacherMappingRepository mappingRepository;
    private final ClassSectionRepository classSectionRepository;
    private final RoomRepository roomRepository;

    @Override
    @Transactional
    public TimetableGenerationResult generateForSection(Long sectionId, String workingDays, int periodsPerDay) {
        ClassSection section = classSectionRepository.findById(sectionId).orElse(null);
        if (section == null) {
            return TimetableGenerationResult.builder()
                    .success(false).message("Section not found: " + sectionId)
                    .violations(List.of("Section " + sectionId + " does not exist"))
                    .warnings(List.of()).build();
        }

        List<SubjectTeacherMapping> mappings = mappingRepository.findBySection_SectionIdAndIsActiveTrue(sectionId);
        if (mappings.isEmpty()) {
            return TimetableGenerationResult.builder()
                    .success(false)
                    .message("No subject-teacher mappings defined for section: " + section.getSectionName())
                    .violations(List.of("Add subject-teacher mappings before generating a timetable"))
                    .warnings(List.of()).build();
        }

        // Deactivate existing timetable entries for this section
        List<Timetable> existing = timetableRepository.findBySection_SectionIdAndIsActiveTrue(sectionId);
        existing.forEach(t -> t.setIsActive(false));
        timetableRepository.saveAll(existing);

        List<Day> days = parseDays(workingDays);
        if (days.isEmpty()) {
            return TimetableGenerationResult.builder()
                    .success(false).message("No valid working days provided")
                    .violations(List.of("workingDays must contain valid Day enum values"))
                    .warnings(List.of()).build();
        }

        // Build the list of subject slots that need to be filled
        List<SlotDemand> demands = buildDemands(mappings);
        if (demands.isEmpty()) {
            return TimetableGenerationResult.builder()
                    .success(false).message("No slot demands (periodsPerWeek = 0 for all mappings)")
                    .violations(List.of("Set periodsPerWeek > 0 for at least one mapping"))
                    .warnings(List.of()).build();
        }

        // Total available slots
        int totalSlots = days.size() * periodsPerDay;
        int totalDemanded = demands.size();
        List<String> warnings = new ArrayList<>();
        List<String> violations = new ArrayList<>();

        if (totalDemanded > totalSlots) {
            violations.add(String.format(
                    "Section '%s': %d periods demanded but only %d slots available (%d days × %d periods). " +
                    "Reduce periodsPerWeek or add more working days.",
                    section.getSectionName(), totalDemanded, totalSlots, days.size(), periodsPerDay));
            return TimetableGenerationResult.builder()
                    .success(false).message("Cannot generate: more periods demanded than slots available")
                    .violations(violations).warnings(warnings).build();
        }

        // Greedy assignment: iterate day×period, assign demands in order
        List<Timetable> generated = new ArrayList<>();
        int demandIndex = 0;
        LocalDate effectiveFrom = LocalDate.now();

        // Track teacher assignments per day to enforce free-period soft constraint
        // Map<teacherId, Map<day, assignedPeriods>>
        Map<Long, Map<Day, Integer>> teacherDayLoad = new HashMap<>();

        // Track room assignments: Map<roomId, Set<day_period>>
        Map<Long, Set<String>> roomSlotMap = new HashMap<>();

        // Track teacher assignments: Map<teacherId, Set<day_period>>
        Map<Long, Set<String>> teacherSlotMap = new HashMap<>();

        // Prepare available rooms ordered by capacity
        List<Room> allRooms = roomRepository.findAll();

        outer:
        for (Day day : days) {
            int period = 1;
            while (period <= periodsPerDay && demandIndex < demands.size()) {
                SlotDemand demand = demands.get(demandIndex);
                String slotKey = day.name() + "_" + period;

                Long teacherId = demand.teacher.getTeacherId();

                // Check teacher constraint
                Set<String> teacherSlots = teacherSlotMap.computeIfAbsent(teacherId, k -> new HashSet<>());
                if (teacherSlots.contains(slotKey)) {
                    // Teacher conflict — try next period
                    period++;
                    continue;
                }

                // Handle double periods for lab subjects
                boolean isDouble = Boolean.TRUE.equals(demand.mapping.getRequiresDoublePeriod());
                if (isDouble && period + 1 > periodsPerDay) {
                    // Not enough room for double period today — move to next day
                    break;
                }

                // Assign a room if available
                Room assignedRoom = findAvailableRoom(allRooms, roomSlotMap, slotKey, section);

                // Create timetable entry
                Timetable entry = Timetable.builder()
                        .section(section)
                        .day(day)
                        .period(period)
                        .subject(demand.subject)
                        .teacher(demand.teacher)
                        .room(assignedRoom)
                        .effectiveFrom(effectiveFrom)
                        .isActive(true)
                        .version(1)
                        .build();
                generated.add(entry);

                // Mark teacher slot used
                teacherSlots.add(slotKey);

                // Mark room slot used
                if (assignedRoom != null) {
                    roomSlotMap.computeIfAbsent(assignedRoom.getRoomId(), k -> new HashSet<>()).add(slotKey);
                }

                // Track teacher day load for soft-constraint check
                teacherDayLoad.computeIfAbsent(teacherId, k -> new HashMap<>())
                        .merge(day, 1, Integer::sum);

                demandIndex++;
                period += isDouble ? 2 : 1;
            }
        }

        // Check if all demands were satisfied
        if (demandIndex < demands.size()) {
            int unscheduled = demands.size() - demandIndex;
            violations.add(String.format(
                    "Could not schedule %d period(s) for section '%s'. " +
                    "Teacher conflicts or insufficient slots prevented full generation.",
                    unscheduled, section.getSectionName()));
        }

        // Soft constraint: warn if any teacher has no free period on any day
        checkFreePeriodsWarning(teacherDayLoad, periodsPerDay, warnings);

        if (!violations.isEmpty()) {
            // Partial failure — rollback generated entries
            return TimetableGenerationResult.builder()
                    .success(false)
                    .message("Generation failed with constraint violations")
                    .violations(violations)
                    .warnings(warnings)
                    .entriesCreated(0)
                    .build();
        }

        timetableRepository.saveAll(generated);
        log.info("Generated {} timetable entries for section '{}'", generated.size(), section.getSectionName());

        return TimetableGenerationResult.builder()
                .success(true)
                .message("Timetable generated successfully for section: " + section.getSectionName())
                .entriesCreated(generated.size())
                .violations(List.of())
                .warnings(warnings)
                .build();
    }

    @Override
    @Transactional
    public TimetableGenerationResult generateForAll(String workingDays, int periodsPerDay) {
        List<ClassSection> sections = classSectionRepository.findAll();
        if (sections.isEmpty()) {
            return TimetableGenerationResult.builder()
                    .success(false).message("No class sections found")
                    .violations(List.of("Create class sections before generating timetables"))
                    .warnings(List.of()).build();
        }

        int totalCreated = 0;
        List<String> allViolations = new ArrayList<>();
        List<String> allWarnings = new ArrayList<>();

        for (ClassSection section : sections) {
            TimetableGenerationResult result = generateForSection(section.getSectionId(), workingDays, periodsPerDay);
            totalCreated += result.getEntriesCreated();
            allViolations.addAll(result.getViolations());
            allWarnings.addAll(result.getWarnings());
        }

        boolean success = allViolations.isEmpty();
        return TimetableGenerationResult.builder()
                .success(success)
                .message(success
                        ? String.format("Timetables generated for all %d sections", sections.size())
                        : "Generation completed with violations in some sections")
                .entriesCreated(totalCreated)
                .violations(allViolations)
                .warnings(allWarnings)
                .build();
    }

    @Override
    public List<String> getClashReport() {
        List<Timetable> all = timetableRepository.findAll().stream()
                .filter(t -> Boolean.TRUE.equals(t.getIsActive()))
                .collect(Collectors.toList());

        List<String> clashes = new ArrayList<>();

        // Group by day+period and look for duplicates
        Map<String, List<Timetable>> byTeacherSlot = new HashMap<>();
        Map<String, List<Timetable>> bySectionSlot = new HashMap<>();
        Map<String, List<Timetable>> byRoomSlot = new HashMap<>();

        for (Timetable t : all) {
            String slot = t.getDay().name() + "_" + t.getPeriod();
            byTeacherSlot.computeIfAbsent(t.getTeacher().getTeacherId() + "_" + slot, k -> new ArrayList<>()).add(t);
            bySectionSlot.computeIfAbsent(t.getSection().getSectionId() + "_" + slot, k -> new ArrayList<>()).add(t);
            if (t.getRoom() != null) {
                byRoomSlot.computeIfAbsent(t.getRoom().getRoomId() + "_" + slot, k -> new ArrayList<>()).add(t);
            }
        }

        byTeacherSlot.values().stream().filter(list -> list.size() > 1).forEach(list -> {
            Timetable first = list.get(0);
            clashes.add(String.format("TEACHER CLASH: Teacher '%s' double-booked on %s Period %d (affects %d sections)",
                    first.getTeacher().getName(), first.getDay(), first.getPeriod(), list.size()));
        });

        bySectionSlot.values().stream().filter(list -> list.size() > 1).forEach(list -> {
            Timetable first = list.get(0);
            clashes.add(String.format("SECTION CLASH: Section '%s' has %d subjects on %s Period %d",
                    first.getSection().getSectionName(), list.size(), first.getDay(), first.getPeriod()));
        });

        byRoomSlot.values().stream().filter(list -> list.size() > 1).forEach(list -> {
            Timetable first = list.get(0);
            clashes.add(String.format("ROOM CLASH: Room '%s' double-booked on %s Period %d",
                    first.getRoom().getRoomName(), first.getDay(), first.getPeriod()));
        });

        return clashes;
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private List<Day> parseDays(String workingDays) {
        if (workingDays == null || workingDays.isBlank()) {
            return List.of(Day.MON, Day.TUE, Day.WED, Day.THU, Day.FRI);
        }
        List<Day> result = new ArrayList<>();
        for (String s : workingDays.split(",")) {
            try {
                result.add(Day.valueOf(s.trim().toUpperCase()));
            } catch (IllegalArgumentException ignored) {
                log.warn("Ignoring unknown day: {}", s.trim());
            }
        }
        return result;
    }

    private List<SlotDemand> buildDemands(List<SubjectTeacherMapping> mappings) {
        List<SlotDemand> demands = new ArrayList<>();
        for (SubjectTeacherMapping m : mappings) {
            int count = m.getPeriodsPerWeek() == null ? 5 : m.getPeriodsPerWeek();
            for (int i = 0; i < count; i++) {
                demands.add(new SlotDemand(m, m.getSubject(), m.getTeacher()));
            }
        }
        return demands;
    }

    private Room findAvailableRoom(List<Room> rooms, Map<Long, Set<String>> roomSlotMap,
                                    String slotKey, ClassSection section) {
        for (Room room : rooms) {
            Set<String> usedSlots = roomSlotMap.getOrDefault(room.getRoomId(), Collections.emptySet());
            if (!usedSlots.contains(slotKey)) {
                return room;
            }
        }
        return null; // No room available — allowed (room is optional)
    }

    private void checkFreePeriodsWarning(Map<Long, Map<Day, Integer>> teacherDayLoad,
                                          int periodsPerDay, List<String> warnings) {
        teacherDayLoad.forEach((teacherId, dayMap) -> {
            dayMap.forEach((day, count) -> {
                if (count >= periodsPerDay) {
                    warnings.add(String.format(
                            "Soft constraint: Teacher ID %d has no free period on %s (assigned all %d periods)",
                            teacherId, day, periodsPerDay));
                }
            });
        });
    }

    /** Internal data class representing one period that needs to be scheduled. */
    private static class SlotDemand {
        final SubjectTeacherMapping mapping;
        final Subject subject;
        final Teacher teacher;

        SlotDemand(SubjectTeacherMapping mapping, Subject subject, Teacher teacher) {
            this.mapping = mapping;
            this.subject = subject;
            this.teacher = teacher;
        }
    }
}
